const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const MANOJ_UID = "manoj_uid_1";
const SONALI_UID = "sonali_uid_2";
const INTRUDER_UID = "intruder_uid_3";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated requests are denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").get());
  await assertFails(unauthDb.collection("chats").get());
  await assertFails(unauthDb.collection("groups").get());
  await assertFails(unauthDb.collection("statuses").get());
  await assertFails(unauthDb.collection("calls").get());
});

test("Users: public profile create/update & strict owner-only private PII isolation", async () => {
  const manojDb = testEnv.authenticatedContext(MANOJ_UID).firestore();
  const sonaliDb = testEnv.authenticatedContext(SONALI_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    manojDb.collection("users").doc(MANOJ_UID).set({
      uid: MANOJ_UID,
      displayName: "Manoj",
      about: "Available",
      online: true,
      discoverable: true,
      lastSeen: now,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Private PII settings strictly isolated
  await assertSucceeds(
    manojDb.collection("users").doc(MANOJ_UID).collection("private").doc("settings").set({
      uid: MANOJ_UID,
      phoneNumber: "+919876543210",
      countryCode: "+91",
      email: "manoj@example.com",
      readReceiptsEnabled: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Sonali can read Manoj's public profile via discoverable query, NOT private settings
  await assertSucceeds(sonaliDb.collection("users").where("discoverable", "==", true).get());
  await assertFails(sonaliDb.collection("users").doc(MANOJ_UID).collection("private").doc("settings").get());
  await assertFails(sonaliDb.collection("users").doc(MANOJ_UID).update({ displayName: "Hacked" }));
  // Shadow field rejected
  await assertFails(manojDb.collection("users").doc(MANOJ_UID).update({ isAdmin: true, updatedAt: new Date(Date.now() - 500) }));
});

test("Chats & Messages: participant isolation, Kotlin payload alignment, and sender edit rules", async () => {
  const manojDb = testEnv.authenticatedContext(MANOJ_UID).firestore();
  const sonaliDb = testEnv.authenticatedContext(SONALI_UID).firestore();
  const intruderDb = testEnv.authenticatedContext(INTRUDER_UID).firestore();
  const now = new Date(Date.now() - 2000);
  const chatId = `${MANOJ_UID}_${SONALI_UID}`;

  // Mirrors ChatRepository.getOrCreateChat checking deterministic chat doc before creation
  await assertSucceeds(manojDb.collection("chats").doc(chatId).get());
  await assertFails(intruderDb.collection("chats").doc(chatId).get());

  await assertSucceeds(
    manojDb.collection("chats").doc(chatId).set({
      participantIds: [MANOJ_UID, SONALI_UID],
      createdBy: MANOJ_UID,
      lastMessage: "",
      lastMessageType: "TEXT",
      typingUserIds: [],
      pinnedBy: [],
      mutedBy: [],
      unreadCounts: { [MANOJ_UID]: 0, [SONALI_UID]: 0 },
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    manojDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").set({
      chatId,
      senderId: MANOJ_UID,
      senderName: "Manoj",
      participantIds: [MANOJ_UID, SONALI_UID],
      type: "TEXT",
      text: "Hello Sonali!",
      deliveryStatus: "SENT",
      timestamp: now,
      updatedAt: now,
    })
  );

  // Scoped queries succeed for participants, fail for intruder
  await assertSucceeds(sonaliDb.collection("chats").where("participantIds", "array-contains", SONALI_UID).get());
  await assertSucceeds(
    sonaliDb.collection("chats").doc(chatId).collection("messages")
      .where("participantIds", "array-contains", SONALI_UID)
      .get()
  );
  await assertFails(intruderDb.collection("chats").doc(chatId).get());
  await assertFails(intruderDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").get());

  // Sonali can update deliveryStatus/reactions, NOT message text
  await assertSucceeds(
    sonaliDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").update({
      deliveryStatus: "READ",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    sonaliDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").update({
      text: "Tampered",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Calls: terminal state locking prevents reopening ENDED/REJECTED calls", async () => {
  const manojDb = testEnv.authenticatedContext(MANOJ_UID).firestore();
  const sonaliDb = testEnv.authenticatedContext(SONALI_UID).firestore();
  const now = new Date(Date.now() - 2000);
  const callId = "call_1";

  await assertSucceeds(
    manojDb.collection("calls").doc(callId).set({
      callerId: MANOJ_UID,
      callerName: "Manoj",
      receiverId: SONALI_UID,
      receiverName: "Sonali",
      participantIds: [MANOJ_UID, SONALI_UID],
      callType: "VIDEO",
      status: "RINGING",
      startedAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    sonaliDb.collection("calls").doc(callId).update({
      status: "ENDED",
      endedAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Terminal state lock rejects reopening
  await assertFails(
    manojDb.collection("calls").doc(callId).update({
      status: "ACCEPTED",
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

test("Contacts & PhoneLookup: owner-only saved contacts and anti-scraping get-only phone lookup", async () => {
  const manojDb = testEnv.authenticatedContext(MANOJ_UID).firestore();
  const sonaliDb = testEnv.authenticatedContext(SONALI_UID).firestore();
  const now = new Date(Date.now() - 1000);

  // Manoj publishes his exact-match phone lookup doc
  await assertSucceeds(
    manojDb.collection("phoneLookup").doc("919876543210").set({
      uid: MANOJ_UID,
      displayName: "Manoj",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Sonali can look up exact phone number via get, but CANNOT list/scrape phoneLookup
  await assertSucceeds(sonaliDb.collection("phoneLookup").doc("919876543210").get());
  await assertFails(sonaliDb.collection("phoneLookup").get());

  // Sonali saves Manoj into her private contacts subcollection
  await assertSucceeds(
    sonaliDb.collection("users").doc(SONALI_UID).collection("contacts").doc(MANOJ_UID).set({
      contactUid: MANOJ_UID,
      displayName: "Manoj",
      addedVia: "PHONE",
      phoneNumber: "+919876543210",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Manoj cannot read Sonali's private saved contacts
  await assertFails(manojDb.collection("users").doc(SONALI_UID).collection("contacts").get());
});

test("Statuses & Profile Photos: text/image stories and compressed inline data-URI avatars succeed", async () => {
  const manojDb = testEnv.authenticatedContext(MANOJ_UID).firestore();
  const sonaliDb = testEnv.authenticatedContext(SONALI_UID).firestore();
  const now = new Date(Date.now() - 1000);
  const expiresAt = new Date(Date.now() + 24 * 60 * 60 * 1000);
  const sampleAvatarDataUri = "data:image/jpeg;base64," + "A".repeat(40000);
  const sampleStoryDataUri = "data:image/jpeg;base64," + "B".repeat(180000);

  // User creates profile with compressed data URI avatar
  await assertSucceeds(
    manojDb.collection("users").doc(MANOJ_UID).set({
      uid: MANOJ_UID,
      displayName: "Manoj",
      photoUrl: sampleAvatarDataUri,
      about: "Available",
      online: true,
      discoverable: true,
      lastSeen: now,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Manoj posts a TEXT story
  await assertSucceeds(
    manojDb.collection("statuses").doc("story_text_1").set({
      userId: MANOJ_UID,
      userName: "Manoj",
      userPhotoUrl: sampleAvatarDataUri,
      caption: "Good morning!",
      type: "TEXT",
      backgroundColorHex: "#4B3FE2",
      privacy: "ALL_CONTACTS",
      viewerIds: [],
      createdAt: now,
      expiresAt,
      updatedAt: now,
    })
  );

  // Manoj posts an IMAGE story with inline compressed data URI
  await assertSucceeds(
    manojDb.collection("statuses").doc("story_img_1").set({
      userId: MANOJ_UID,
      userName: "Manoj",
      userPhotoUrl: sampleAvatarDataUri,
      caption: "",
      mediaUrl: sampleStoryDataUri,
      type: "IMAGE",
      privacy: "ALL_CONTACTS",
      viewerIds: [],
      createdAt: now,
      expiresAt,
      updatedAt: now,
    })
  );

  // Sonali can list ALL_CONTACTS stories and mark viewed
  await assertSucceeds(sonaliDb.collection("statuses").where("privacy", "==", "ALL_CONTACTS").get());
  await assertSucceeds(
    sonaliDb.collection("statuses").doc("story_img_1").update({
      viewerIds: [SONALI_UID],
      updatedAt: new Date(Date.now() - 200),
    })
  );
});



package com.example.utils

data class CountryCode(
    val name: String,
    val isoCode: String,
    val dialCode: String,
    val flagEmoji: String
)

object CountryCodes {
    val all: List<CountryCode> = listOf(
        CountryCode("India", "IN", "+91", "\uD83C\uDDEE\uD83C\uDDF3"),
        CountryCode("United States", "US", "+1", "\uD83C\uDDFA\uD83C\uDDF8"),
        CountryCode("United Kingdom", "GB", "+44", "\uD83C\uDDEC\uD83C\uDDE7"),
        CountryCode("Canada", "CA", "+1", "\uD83C\uDDE8\uD83C\uDDE6"),
        CountryCode("Australia", "AU", "+61", "\uD83C\uDDE6\uD83C\uDDFA"),
        CountryCode("United Arab Emirates", "AE", "+971", "\uD83C\uDDE6\uD83C\uDDEA"),
        CountryCode("Singapore", "SG", "+65", "\uD83C\uDDF8\uD83C\uDDEC"),
        CountryCode("Germany", "DE", "+49", "\uD83C\uDDE9\uD83C\uDDEA"),
        CountryCode("France", "FR", "+33", "\uD83C\uDDEB\uD83C\uDDF7"),
        CountryCode("Japan", "JP", "+81", "\uD83C\uDDEF\uD83C\uDDF5"),
        CountryCode("South Korea", "KR", "+82", "\uD83C\uDDF0\uD83C\uDDF7"),
        CountryCode("Brazil", "BR", "+55", "\uD83C\uDDE7\uD83C\uDDF7"),
        CountryCode("Mexico", "MX", "+52", "\uD83C\uDDF2\uD83C\uDDFD"),
        CountryCode("Indonesia", "ID", "+62", "\uD83C\uDDEE\uD83C\uDDE9"),
        CountryCode("Malaysia", "MY", "+60", "\uD83C\uDDF2\uD83C\uDDFE"),
        CountryCode("Nepal", "NP", "+977", "\uD83C\uDDF3\uD83C\uDDF5"),
        CountryCode("Bangladesh", "BD", "+880", "\uD83C\uDDE7\uD83C\uDDE9"),
        CountryCode("Sri Lanka", "LK", "+94", "\uD83C\uDDF1\uD83C\uDDF0"),
        CountryCode("Saudi Arabia", "SA", "+966", "\uD83C\uDDF8\uD83C\uDDE6"),
        CountryCode("Qatar", "QA", "+974", "\uD83C\uDDF6\uD83C\uDDE6"),
        CountryCode("Netherlands", "NL", "+31", "\uD83C\uDDF3\uD83C\uDDF1"),
        CountryCode("Italy", "IT", "+39", "\uD83C\uDDEE\uD83C\uDDF9"),
        CountryCode("Spain", "ES", "+34", "\uD83C\uDDEA\uD83C\uDDF8"),
        CountryCode("Switzerland", "CH", "+41", "\uD83C\uDDE8\uD83C\uDDED"),
        CountryCode("Sweden", "SE", "+46", "\uD83C\uDDF8\uD83C\uDDEA"),
        CountryCode("New Zealand", "NZ", "+64", "\uD83C\uDDF3\uD83C\uDDFF"),
        CountryCode("South Africa", "ZA", "+27", "\uD83C\uDDFF\uD83C\uDDE6"),
        CountryCode("Nigeria", "NG", "+234", "\uD83C\uDDF3\uD83C\uDDEC"),
        CountryCode("Kenya", "KE", "+254", "\uD83C\uDDF0\uD83C\uDDEA"),
        CountryCode("Philippines", "PH", "+63", "\uD83C\uDDF5\uD83C\uDDED"),
        CountryCode("Thailand", "TH", "+66", "\uD83C\uDDF9\uD83C\uDDED"),
        CountryCode("Vietnam", "VN", "+84", "\uD83C\uDDFB\uD83C\uDDF3"),
        CountryCode("Ireland", "IE", "+353", "\uD83C\uDDEE\uD83C\uDDEA"),
        CountryCode("Poland", "PL", "+48", "\uD83C\uDDF5\uD83C\uDDF1"),
        CountryCode("Turkey", "TR", "+90", "\uD83C\uDDF9\uD83C\uDDF7")
    )
}

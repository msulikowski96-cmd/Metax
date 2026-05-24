package com.example.data

// Enums for precise metabolic computations
enum class Gender(val displayName: String, val icon: String) {
    MALE("Mężczyzna", "♂"),
    FEMALE("Kobieta", "♀")
}

enum class BmrFormula(val displayName: String, val detailedDesc: String) {
    MIFFLIN_ST_JEOR("Mifflin-St Jeor", "Współczesny standard naukowy, zalecany dla większości osób."),
    KATCH_MCARDLE("Katch-McArdle", "Najdokładniejszy dla osób o znanej zawartości tkanki tłuszczowej (oparty na LBM)."),
    HARRIS_BENEDICT("Harris-Benedict (Klasyczny)", "Tradycyjny wzór, niezawodny dla osób średnio aktywnych.")
}

enum class DietType(val displayName: String, val detailedDesc: String) {
    BALANCED("Zrównoważona (Standard)", "Białko: 2.0g/kg, Tłuszcze: 25%, Węglowodany: reszta energii. Ogólnorozwojowa."),
    KETO("Ketogeniczna / Niskowęglowodanowa", "Białko: 2.0g/kg, Węglowodany: do 40g, Tłuszcze: reszta energii (ok. 70%+)."),
    HIGH_PROTEIN("Wysokobiałkowa (Sportowa)", "Białko: 2.5g/kg, Tłuszcze: 20%, Węglowodany: reszta (Dla budujących mięśnie)."),
    LOW_FAT("Niskotłuszczowa", "Białko: 2.0g/kg, Tłuszcze: 15%, Węglowodany: reszta (Wspomaga wydolność tlenową).")
}

enum class ActivityLevel(val multiplier: Double, val displayName: String, val detailedDesc: String) {
    SEDENTARY(1.2, "Brak aktywności (Siedzący)", "Praca przy biurku, brak planowanych treningów"),
    LIGHTLY_ACTIVE(1.375, "Niska aktywność (1-2 tren/tyg)", "Spacery, lekki sport raz lub dwa razy w tygodniu"),
    MODERATELY_ACTIVE(1.55, "Umiarkowana (3-4 tren/tyg)", "Regularne treningi, umiarkowana aktywność codzienna"),
    VERY_ACTIVE(1.725, "Wysoka aktywność (5-6 tren/tyg)", "Intensywne treningi prawie codziennie, aktywny tryb życia"),
    EXTRA_ACTIVE(1.9, "Ekstremalna (Codzienny sport/fizyczna)", "Ciężka praca fizyczna lub codzienne wyczynowe treningi")
}

enum class FitnessGoal(val deltaCalories: Int, val displayName: String, val detailedDesc: String) {
    EXTREME_LOSE(-600, "Szybka redukcja (Duży deficyt)", "Szybka utrata masy ciała. Krótkoterminowa, wymagająca skupienia."),
    LOSE_WEIGHT(-350, "Zdrowa redukcja (Zalecana)", "Bezpieczna i stabilna utrata tkanki tłuszczowej bez utraty mięśni."),
    MAINTAIN(0, "Utrzymanie wagi (Zero)", "Utrzymanie obecnej masy ciała, stabilizacja i regeneracja."),
    LEAN_GAIN(200, "Powolna masa (Lean Bulk)", "Konstruktywna budowa mięśni przy minimalnym przyroście tłuszczu."),
    GAIN_WEIGHT(450, "Szybka masa (Nadwyżka)", "Maksymalny rozwój siły i masy mięśniowej z nadwyżką energii.")
}

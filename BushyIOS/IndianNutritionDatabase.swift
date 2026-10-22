import Foundation

public struct IndianNutritionEntry: Identifiable, Codable, Equatable {
    public var id: String
    public var name: String
    public var aliases: [String]
    public var caloriesPer100g: Int
    public var proteinPer100g: Double
    public var carbsPer100g: Double
    public var fatPer100g: Double
    public var defaultUnit: PortionUnit
    public var defaultUnitWeightGrams: Double
    public var prepNotes: String
    public var regionOrCategory: String
    public var source: String
    
    public init(
        id: String,
        name: String,
        aliases: [String],
        caloriesPer100g: Int,
        proteinPer100g: Double,
        carbsPer100g: Double,
        fatPer100g: Double,
        defaultUnit: PortionUnit,
        defaultUnitWeightGrams: Double,
        prepNotes: String,
        regionOrCategory: String = "Pan-India",
        source: String = "Anuvaad INDB / ICMR Nutrition Tables"
    ) {
        self.id = id
        self.name = name
        self.aliases = aliases
        self.caloriesPer100g = caloriesPer100g
        self.proteinPer100g = proteinPer100g
        self.carbsPer100g = carbsPer100g
        self.fatPer100g = fatPer100g
        self.defaultUnit = defaultUnit
        self.defaultUnitWeightGrams = defaultUnitWeightGrams
        self.prepNotes = prepNotes
        self.regionOrCategory = regionOrCategory
        self.source = source
    }
}

public struct IndianNutritionDatabase {
    public static let items: [IndianNutritionEntry] = [
        IndianNutritionEntry(
            id: "dal_tadka",
            name: "Dal Tadka (Yellow Dal)",
            aliases: ["dal", "yellow dal", "arhar dal", "toor dal", "dal fry", "daal", "दाल", "दाल तड़का", "वरण", "ডাল", "பருப்பு", "పప్పు", "દાળ", "ਦਾਲ"],
            caloriesPer100g: 105, proteinPer100g: 5.2, carbsPer100g: 14.5, fatPer100g: 3.2,
            defaultUnit: .katori, defaultUnitWeightGrams: 150.0,
            prepNotes: "Lentils cooked with ghee/oil tadka (mustard seeds, cumin, garlic, chili).", regionOrCategory: "North & Central India"
        ),
        IndianNutritionEntry(
            id: "dal_makhani",
            name: "Dal Makhani",
            aliases: ["makhani dal", "black dal", "urad dal", "makhni dal", "दाल मखनी", "মাখন ডাল", "தால் மக்கனி", "దాల్ మఖానీ"],
            caloriesPer100g: 150, proteinPer100g: 5.8, carbsPer100g: 16.0, fatPer100g: 7.5,
            defaultUnit: .katori, defaultUnitWeightGrams: 150.0,
            prepNotes: "Slow-cooked black lentils with butter, cream, and tomato gravy.", regionOrCategory: "Punjabi / North India"
        ),
        IndianNutritionEntry(
            id: "basmati_rice",
            name: "Steamed Basmati Rice",
            aliases: ["rice", "chawal", "steamed rice", "white rice", "bhat", "sadam", "चावल", "भात", "ভাত", "அரிசி", "అన్నం", "ચોખા"],
            caloriesPer100g: 130, proteinPer100g: 2.7, carbsPer100g: 28.0, fatPer100g: 0.3,
            defaultUnit: .katori, defaultUnitWeightGrams: 150.0,
            prepNotes: "Plain boiled white basmati rice without butter/oil.", regionOrCategory: "Pan-India"
        ),
        IndianNutritionEntry(
            id: "roti_chapati",
            name: "Whole Wheat Roti / Chapati",
            aliases: ["roti", "chapati", "phulka", "wheat roti", "chappati", "rotli", "रोटी", "चपाती", "फुल्का", "রুটি", "ரொட்டி", "రొట్టి"],
            caloriesPer100g: 260, proteinPer100g: 9.0, carbsPer100g: 50.0, fatPer100g: 2.5,
            defaultUnit: .piece, defaultUnitWeightGrams: 35.0,
            prepNotes: "1 medium roti (~35g) has ~90 kcal. Ghee application adds ~35 kcal per tsp.", regionOrCategory: "Pan-India"
        ),
        IndianNutritionEntry(
            id: "butter_naan",
            name: "Butter Naan",
            aliases: ["naan", "butter naan", "garlic naan", "नान", "बटर नान", "નાન"],
            caloriesPer100g: 310, proteinPer100g: 8.5, carbsPer100g: 52.0, fatPer100g: 8.0,
            defaultUnit: .piece, defaultUnitWeightGrams: 90.0,
            prepNotes: "Leavened maida bread baked in tandoor with brushed butter.", regionOrCategory: "North India"
        ),
        IndianNutritionEntry(
            id: "paneer_butter_masala",
            name: "Paneer Butter Masala",
            aliases: ["paneer masala", "shahi paneer", "paneer butter", "paneer curry", "panner", "paner", "पनीर", "पनीर बटर मसाला", "পনীর", "பன்னீர்", "పనీర్"],
            caloriesPer100g: 210, proteinPer100g: 8.0, carbsPer100g: 10.0, fatPer100g: 15.5,
            defaultUnit: .katori, defaultUnitWeightGrams: 150.0,
            prepNotes: "Cottage cheese cubes in rich tomato, cream, cashew, and butter gravy.", regionOrCategory: "North India"
        ),
        IndianNutritionEntry(
            id: "palak_paneer",
            name: "Palak Paneer",
            aliases: ["spinach paneer", "palak", "spinach cottage cheese", "पालक पनीर", "পালং পনীর", "பாலக் பன்னீர்", "పాలక్ పనీర్"],
            caloriesPer100g: 145, proteinPer100g: 7.5, carbsPer100g: 6.0, fatPer100g: 10.5,
            defaultUnit: .katori, defaultUnitWeightGrams: 150.0,
            prepNotes: "Pureed spinach sauce cooked with paneer cubes and Indian spices.", regionOrCategory: "North India"
        ),
        IndianNutritionEntry(
            id: "rajma_masala",
            name: "Rajma Masala",
            aliases: ["rajma", "kidney beans curry", "rajma chawal", "राजमा", "রাজমা", "ராஜ்மா", "రాజ్మా"],
            caloriesPer100g: 125, proteinPer100g: 5.5, carbsPer100g: 18.0, fatPer100g: 3.5,
            defaultUnit: .katori, defaultUnitWeightGrams: 150.0,
            prepNotes: "Red kidney beans simmered in onion-tomato masala gravy.", regionOrCategory: "Punjabi / North India"
        ),
        IndianNutritionEntry(
            id: "chana_masala",
            name: "Chana Masala / Chole",
            aliases: ["chole", "chana", "chickpea curry", "kabuli chana", "छोले", "चना मसाला", "ছোলা", "சுண்டல்", "శనగలు"],
            caloriesPer100g: 140, proteinPer100g: 6.2, carbsPer100g: 20.0, fatPer100g: 4.0,
            defaultUnit: .katori, defaultUnitWeightGrams: 150.0,
            prepNotes: "White chickpeas cooked in spiced tangy tomato onion gravy.", regionOrCategory: "North India"
        ),
        IndianNutritionEntry(
            id: "masala_dosa",
            name: "Masala Dosa",
            aliases: ["dosa", "masala dosa", "plain dosa", "dosai", "dosaa", "डोसा", "मसाला डोसा", "தோசை", "దోశ", "દોસા"],
            caloriesPer100g: 190, proteinPer100g: 4.2, carbsPer100g: 29.0, fatPer100g: 6.5,
            defaultUnit: .piece, defaultUnitWeightGrams: 150.0,
            prepNotes: "Fermented rice & urad dal crepe stuffed with spiced potato masala.", regionOrCategory: "South India"
        ),
        IndianNutritionEntry(
            id: "idli_sambar",
            name: "Steamed Idli (Plain)",
            aliases: ["idli", "steamed idli", "rice idli", "idly", "इडली", "இட்லி", "ఇడ్లీ", "ઈડલી"],
            caloriesPer100g: 135, proteinPer100g: 4.5, carbsPer100g: 27.5, fatPer100g: 0.5,
            defaultUnit: .piece, defaultUnitWeightGrams: 50.0,
            prepNotes: "Steamed fermented rice and lentil cake. Very low in fat.", regionOrCategory: "South India"
        ),
        IndianNutritionEntry(
            id: "sambar",
            name: "South Indian Sambar",
            aliases: ["sambar", "sambhar", "சாம்பார்", "సాంబార్", "સાંભાર", "सांबर"],
            caloriesPer100g: 65, proteinPer100g: 3.0, carbsPer100g: 10.0, fatPer100g: 1.5,
            defaultUnit: .katori, defaultUnitWeightGrams: 150.0,
            prepNotes: "Toor dal stew cooked with mixed vegetables, tamarind, and aromatic sambar powder.", regionOrCategory: "South India"
        ),
        IndianNutritionEntry(
            id: "rasam",
            name: "South Indian Rasam",
            aliases: ["rasam", "saaru", "ரசம்", "చారు", "రసం"],
            caloriesPer100g: 35, proteinPer100g: 1.2, carbsPer100g: 5.5, fatPer100g: 0.8,
            defaultUnit: .katori, defaultUnitWeightGrams: 150.0,
            prepNotes: "Tangy pepper and tamarind lentil soup with cumin and garlic.", regionOrCategory: "South India"
        ),
        IndianNutritionEntry(
            id: "biryani_chicken",
            name: "Chicken Biryani",
            aliases: ["biryani", "chicken biryani", "hyderabadi biryani", "biryany", "बिरयानी", "চিকেন বিরিয়ানি", "பிரியாணி", "బిర్యానీ"],
            caloriesPer100g: 185, proteinPer100g: 9.5, carbsPer100g: 22.0, fatPer100g: 6.8,
            defaultUnit: .cup, defaultUnitWeightGrams: 200.0,
            prepNotes: "Fragrant basmati rice layered with marinated chicken, ghee, and aromatic spices.", regionOrCategory: "Hyderabadi / Pan-India"
        ),
        IndianNutritionEntry(
            id: "poha",
            name: "Kanda Batata Poha",
            aliases: ["poha", "flattened rice", "kanda poha", "pohe", "पोहा", "पोहे", "চিঁড়ে", "அவல்", "అటుకులు"],
            caloriesPer100g: 160, proteinPer100g: 3.2, carbsPer100g: 28.0, fatPer100g: 4.2,
            defaultUnit: .katori, defaultUnitWeightGrams: 120.0,
            prepNotes: "Flattened rice tempered with mustard seeds, curry leaves, onions, peanuts, and turmeric.", regionOrCategory: "Maharashtra & MP"
        ),
        IndianNutritionEntry(
            id: "vada_pav",
            name: "Mumbai Vada Pav",
            aliases: ["vada pav", "wada pav", "mumbai vada pav", "वाडा पाव", "વડાપાવ"],
            caloriesPer100g: 230, proteinPer100g: 5.0, carbsPer100g: 34.0, fatPer100g: 8.5,
            defaultUnit: .piece, defaultUnitWeightGrams: 130.0,
            prepNotes: "Deep-fried spiced potato dumpling inside a soft pav bun with garlic chutney.", regionOrCategory: "Maharashtra"
        ),
        IndianNutritionEntry(
            id: "misal_pav",
            name: "Kolhapuri Misal Pav",
            aliases: ["misal", "misal pav", "मिसळ पाव", "मिसळ"],
            caloriesPer100g: 180, proteinPer100g: 6.8, carbsPer100g: 22.0, fatPer100g: 7.5,
            defaultUnit: .katori, defaultUnitWeightGrams: 200.0,
            prepNotes: "Spicy sprouted moth bean curry topped with farsan, onions, and served with pav bread.", regionOrCategory: "Maharashtra"
        ),
        IndianNutritionEntry(
            id: "aloo_paratha",
            name: "Aloo Paratha",
            aliases: ["aloo paratha", "paratha", "potato paratha", "आलू पराठा"],
            caloriesPer100g: 240, proteinPer100g: 5.5, carbsPer100g: 36.0, fatPer100g: 8.5,
            defaultUnit: .piece, defaultUnitWeightGrams: 110.0,
            prepNotes: "Whole wheat flatbread stuffed with spiced mashed potatoes, shallow fried in ghee/butter.", regionOrCategory: "North India"
        ),
        IndianNutritionEntry(
            id: "plain_curd_dahi",
            name: "Plain Curd / Dahi",
            aliases: ["dahi", "curd", "yogurt", "plain yogurt", "दही", "দই", "தயிர்", "పెరుగు"],
            caloriesPer100g: 60, proteinPer100g: 3.5, carbsPer100g: 4.7, fatPer100g: 3.0,
            defaultUnit: .katori, defaultUnitWeightGrams: 120.0,
            prepNotes: "Fresh whole milk set curd / dahi.", regionOrCategory: "Pan-India"
        ),
        IndianNutritionEntry(
            id: "upma",
            name: "Rava Upma",
            aliases: ["upma", "rava upma", "sooji upma", "उपमा", "உப்புமா", "ఉప్మా"],
            caloriesPer100g: 155, proteinPer100g: 3.8, carbsPer100g: 26.0, fatPer100g: 4.2,
            defaultUnit: .katori, defaultUnitWeightGrams: 150.0,
            prepNotes: "Roasted semolina cooked with vegetables, mustard seeds, curry leaves, and cashews.", regionOrCategory: "South India"
        ),
        IndianNutritionEntry(
            id: "dhokla",
            name: "Khaman Dhokla",
            aliases: ["dhokla", "khaman", "besan dhokla", "ढोकला", "ઢોકળા"],
            caloriesPer100g: 160, proteinPer100g: 6.5, carbsPer100g: 23.0, fatPer100g: 5.0,
            defaultUnit: .piece, defaultUnitWeightGrams: 50.0,
            prepNotes: "Steamed fermented gram flour sponge tempered with mustard seeds and green chili.", regionOrCategory: "Gujarat"
        ),
        IndianNutritionEntry(
            id: "chicken_curry",
            name: "Indian Chicken Curry",
            aliases: ["chicken curry", "chicken gravy", "murgh curry", "चिकन करी", "চিকেন কারি", "கோழி குழம்பு"],
            caloriesPer100g: 165, proteinPer100g: 14.0, carbsPer100g: 5.0, fatPer100g: 9.5,
            defaultUnit: .katori, defaultUnitWeightGrams: 180.0,
            prepNotes: "Home-style tender chicken simmered with ginger, garlic, onions, and ground spices.", regionOrCategory: "Pan-India"
        )
    ]
    
    public static func search(query: String) -> [IndianNutritionEntry] {
        let clean = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !clean.isEmpty else { return items }
        return items.filter { entry in
            entry.name.lowercased().contains(clean) ||
            entry.aliases.contains { $0.lowercased().contains(clean) }
        }
    }
}

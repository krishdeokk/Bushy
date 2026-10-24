package com.bushy.health

import java.util.UUID

data class IndianNutritionEntry(
    val id: String,
    val name: String,
    val aliases: List<String>,
    val caloriesPer100g: Int,
    val proteinPer100g: Float, // grams
    val carbsPer100g: Float, // grams
    val fatPer100g: Float, // grams
    val defaultUnit: PortionUnit,
    val defaultUnitWeightGrams: Float,
    val prepNotes: String,
    val regionOrCategory: String = "Pan-India",
    val source: String = "Anuvaad INDB / ICMR Nutrition Tables"
)

object IndianNutritionDatabase {

    private val customEntries = mutableListOf<IndianNutritionEntry>()

    val defaultItems: List<IndianNutritionEntry> = listOf(
        IndianNutritionEntry(
            id = "dal_tadka",
            name = "Dal Tadka (Yellow Dal)",
            aliases = listOf("dal", "yellow dal", "arhar dal", "toor dal", "dal fry", "daal", "दाल", "दाल तड़का", "वरण", "ডাল", "பருப்பு", "పప్పు", "દાળ", "ਦਾਲ"),
            caloriesPer100g = 105, proteinPer100g = 5.2f, carbsPer100g = 14.5f, fatPer100g = 3.2f,
            defaultUnit = PortionUnit.KATORI, defaultUnitWeightGrams = 150f,
            prepNotes = "Lentils cooked with ghee/oil tadka (mustard seeds, cumin, garlic, chili).", regionOrCategory = "North & Central India"
        ),
        IndianNutritionEntry(
            id = "dal_makhani",
            name = "Dal Makhani",
            aliases = listOf("makhani dal", "black dal", "urad dal", "makhni dal", "दाल मखनी", "মাখন ডাল", "தால் மக்கனி", "దాల్ మఖానీ"),
            caloriesPer100g = 150, proteinPer100g = 5.8f, carbsPer100g = 16.0f, fatPer100g = 7.5f,
            defaultUnit = PortionUnit.KATORI, defaultUnitWeightGrams = 150f,
            prepNotes = "Slow-cooked black lentils with butter, cream, and tomato gravy.", regionOrCategory = "Punjabi / North India"
        ),
        IndianNutritionEntry(
            id = "basmati_rice",
            name = "Steamed Basmati Rice",
            aliases = listOf("rice", "chawal", "steamed rice", "white rice", "bhat", "sadam", "चावल", "भात", "ভাত", "அரிசி", "అన్నం", "ચોખા"),
            caloriesPer100g = 130, proteinPer100g = 2.7f, carbsPer100g = 28.0f, fatPer100g = 0.3f,
            defaultUnit = PortionUnit.KATORI, defaultUnitWeightGrams = 150f,
            prepNotes = "Plain boiled white basmati rice without butter/oil.", regionOrCategory = "Pan-India"
        ),
        IndianNutritionEntry(
            id = "roti_chapati",
            name = "Whole Wheat Roti / Chapati",
            aliases = listOf("roti", "chapati", "phulka", "wheat roti", "chappati", "rotli", "रोटी", "चपाती", "फुल्का", "রুটি", "ரொட்டி", "రొట్టి"),
            caloriesPer100g = 260, proteinPer100g = 9.0f, carbsPer100g = 50.0f, fatPer100g = 2.5f,
            defaultUnit = PortionUnit.PIECE, defaultUnitWeightGrams = 35f,
            prepNotes = "1 medium roti (~35g) has ~90 kcal. Ghee application adds ~35 kcal per tsp.", regionOrCategory = "Pan-India"
        ),
        IndianNutritionEntry(
            id = "butter_naan",
            name = "Butter Naan",
            aliases = listOf("naan", "butter naan", "garlic naan", "नान", "बटर नान", "નાન"),
            caloriesPer100g = 310, proteinPer100g = 8.5f, carbsPer100g = 52.0f, fatPer100g = 8.0f,
            defaultUnit = PortionUnit.PIECE, defaultUnitWeightGrams = 90f,
            prepNotes = "Leavened maida bread baked in tandoor with brushed butter.", regionOrCategory = "North India"
        ),
        IndianNutritionEntry(
            id = "paneer_butter_masala",
            name = "Paneer Butter Masala",
            aliases = listOf("paneer masala", "shahi paneer", "paneer butter", "paneer curry", "panner", "paner", "पनीर", "पनीर बटर मसाला", "পনীর", "பன்னீர்", "పనీర్"),
            caloriesPer100g = 210, proteinPer100g = 8.0f, carbsPer100g = 10.0f, fatPer100g = 15.5f,
            defaultUnit = PortionUnit.KATORI, defaultUnitWeightGrams = 150f,
            prepNotes = "Cottage cheese cubes in rich tomato, cream, cashew, and butter gravy.", regionOrCategory = "North India"
        ),
        IndianNutritionEntry(
            id = "palak_paneer",
            name = "Palak Paneer",
            aliases = listOf("spinach paneer", "palak", "spinach cottage cheese", "पालक पनीर", "পালং পনীর", "பாலக் பன்னீர்", "పాలక్ పనీర్"),
            caloriesPer100g = 145, proteinPer100g = 7.5f, carbsPer100g = 6.0f, fatPer100g = 10.5f,
            defaultUnit = PortionUnit.KATORI, defaultUnitWeightGrams = 150f,
            prepNotes = "Pureed spinach sauce cooked with paneer cubes and Indian spices.", regionOrCategory = "North India"
        ),
        IndianNutritionEntry(
            id = "rajma_masala",
            name = "Rajma Masala",
            aliases = listOf("rajma", "kidney beans curry", "rajma chawal", "राजमा", "রাজমা", "ராஜ்மா", "రాజ్మా"),
            caloriesPer100g = 125, proteinPer100g = 5.5f, carbsPer100g = 18.0f, fatPer100g = 3.5f,
            defaultUnit = PortionUnit.KATORI, defaultUnitWeightGrams = 150f,
            prepNotes = "Red kidney beans simmered in onion-tomato masala gravy.", regionOrCategory = "Punjabi / North India"
        ),
        IndianNutritionEntry(
            id = "chana_masala",
            name = "Chana Masala / Chole",
            aliases = listOf("chole", "chana", "chickpea curry", "kabuli chana", "छोले", "चना मसाला", "ছোলা", "சுண்டல்", "శనగలు"),
            caloriesPer100g = 140, proteinPer100g = 6.2f, carbsPer100g = 20.0f, fatPer100g = 4.0f,
            defaultUnit = PortionUnit.KATORI, defaultUnitWeightGrams = 150f,
            prepNotes = "White chickpeas cooked in spiced tangy tomato onion gravy.", regionOrCategory = "North India"
        ),
        IndianNutritionEntry(
            id = "masala_dosa",
            name = "Masala Dosa",
            aliases = listOf("dosa", "masala dosa", "plain dosa", "dosai", "dosaa", "डोसा", "मसाला डोसा", "தோசை", "దోశ", "દોસા"),
            caloriesPer100g = 190, proteinPer100g = 4.2f, carbsPer100g = 29.0f, fatPer100g = 6.5f,
            defaultUnit = PortionUnit.PIECE, defaultUnitWeightGrams = 150f,
            prepNotes = "Fermented rice & urad dal crepe stuffed with spiced potato masala.", regionOrCategory = "South India"
        ),
        IndianNutritionEntry(
            id = "idli_sambar",
            name = "Steamed Idli (Plain)",
            aliases = listOf("idli", "steamed idli", "rice idli", "idly", "इडली", "இட்லி", "ఇడ్లీ", "ઈડલી"),
            caloriesPer100g = 135, proteinPer100g = 4.5f, carbsPer100g = 27.5f, fatPer100g = 0.5f,
            defaultUnit = PortionUnit.PIECE, defaultUnitWeightGrams = 50f,
            prepNotes = "Steamed fermented rice and lentil cake. Very low in fat.", regionOrCategory = "South India"
        ),
        IndianNutritionEntry(
            id = "sambar",
            name = "South Indian Sambar",
            aliases = listOf("sambar", "sambhar", "சாம்பார்", "సాంబార్", "સાંભાર", "सांबर"),
            caloriesPer100g = 65, proteinPer100g = 3.0f, carbsPer100g = 10.0f, fatPer100g = 1.5f,
            defaultUnit = PortionUnit.KATORI, defaultUnitWeightGrams = 150f,
            prepNotes = "Toor dal stew cooked with mixed vegetables, tamarind, and aromatic sambar powder.", regionOrCategory = "South India"
        ),
        IndianNutritionEntry(
            id = "rasam",
            name = "South Indian Rasam",
            aliases = listOf("rasam", "saaru", "ரசம்", "చారు", "రసం"),
            caloriesPer100g = 35, proteinPer100g = 1.2f, carbsPer100g = 5.5f, fatPer100g = 0.8f,
            defaultUnit = PortionUnit.KATORI, defaultUnitWeightGrams = 150f,
            prepNotes = "Tangy pepper and tamarind lentil soup with cumin and garlic.", regionOrCategory = "South India"
        ),
        IndianNutritionEntry(
            id = "biryani_chicken",
            name = "Chicken Biryani",
            aliases = listOf("biryani", "chicken biryani", "hyderabadi biryani", "biryany", "बिरयानी", "চিকেন বিরিয়ানি", "பிரியாணி", "బిర్యానీ"),
            caloriesPer100g = 185, proteinPer100g = 9.5f, carbsPer100g = 22.0f, fatPer100g = 6.8f,
            defaultUnit = PortionUnit.CUP, defaultUnitWeightGrams = 200f,
            prepNotes = "Fragrant basmati rice layered with marinated chicken, ghee, and aromatic spices.", regionOrCategory = "Hyderabadi / Pan-India"
        ),
        IndianNutritionEntry(
            id = "poha",
            name = "Kanda Batata Poha",
            aliases = listOf("poha", "flattened rice", "kanda poha", "pohe", "पोहा", "पोहे", "চিঁড়ে", "அவல்", "அటుకులు"),
            caloriesPer100g = 160, proteinPer100g = 3.2f, carbsPer100g = 28.0f, fatPer100g = 4.2f,
            defaultUnit = PortionUnit.KATORI, defaultUnitWeightGrams = 120f,
            prepNotes = "Flattened rice tempered with mustard seeds, curry leaves, onions, peanuts, and turmeric.", regionOrCategory = "Maharashtra & MP"
        ),
        IndianNutritionEntry(
            id = "vada_pav",
            name = "Mumbai Vada Pav",
            aliases = listOf("vada pav", "wada pav", "mumbai vada pav", "वाडा पाव", "वडಾಪಾವ್"),
            caloriesPer100g = 230, proteinPer100g = 5.0f, carbsPer100g = 34.0f, fatPer100g = 8.5f,
            defaultUnit = PortionUnit.PIECE, defaultUnitWeightGrams = 130f,
            prepNotes = "Deep-fried spiced potato dumpling inside a soft pav bun with garlic chutney.", regionOrCategory = "Maharashtra"
        ),
        IndianNutritionEntry(
            id = "misal_pav",
            name = "Kolhapuri Misal Pav",
            aliases = listOf("misal", "misal pav", "मिसळ पाव", "मिसळ"),
            caloriesPer100g = 180, proteinPer100g = 6.8f, carbsPer100g = 22.0f, fatPer100g = 7.5f,
            defaultUnit = PortionUnit.KATORI, defaultUnitWeightGrams = 200f,
            prepNotes = "Spicy sprouted moth bean curry topped with farsan, onions, and served with pav bread.", regionOrCategory = "Maharashtra"
        ),
        IndianNutritionEntry(
            id = "sabudana_khichdi",
            name = "Sabudana Khichdi",
            aliases = listOf("sabudana", "sabu khichdi", "sago khichdi", "साबुदाना खिचड़ी"),
            caloriesPer100g = 210, proteinPer100g = 2.5f, carbsPer100g = 38.0f, fatPer100g = 6.0f,
            defaultUnit = PortionUnit.KATORI, defaultUnitWeightGrams = 150f,
            prepNotes = "Tapioca pearls sauteed with roasted peanuts, cumin, potatoes, and green chilies.", regionOrCategory = "Maharashtra & MP"
        ),
        IndianNutritionEntry(
            id = "aloo_gobi_sabzi",
            name = "Mixed Vegetable Sabzi (Aloo Gobi)",
            aliases = listOf("sabzi", "aloo gobi", "veg sabzi", "bhindi", "mix veg", "sabji", "सब्जी", "आलू गोभी", "সবজি"),
            caloriesPer100g = 110, proteinPer100g = 2.8f, carbsPer100g = 13.5f, fatPer100g = 5.2f,
            defaultUnit = PortionUnit.KATORI, defaultUnitWeightGrams = 120f,
            prepNotes = "Dry stir-fried potatoes, cauliflower, or seasonal veggies with cumin and spices.", regionOrCategory = "Pan-India"
        ),
        IndianNutritionEntry(
            id = "kadhi_pakora",
            name = "Punjabi Kadhi Pakora",
            aliases = listOf("kadhi", "kadhi pakoda", "karhi", "कढ़ी पकौड़ा", "કઢી"),
            caloriesPer100g = 120, proteinPer100g = 3.8f, carbsPer100g = 11.5f, fatPer100g = 6.5f,
            defaultUnit = PortionUnit.KATORI, defaultUnitWeightGrams = 150f,
            prepNotes = "Tangy yogurt and besan (gram flour) gravy simmered with fried onion-besan fritters.", regionOrCategory = "North & West India"
        ),
        IndianNutritionEntry(
            id = "matar_paneer",
            name = "Matar Paneer",
            aliases = listOf("muttar paneer", "peas paneer", "मटर पनीर", "মটর পনীর"),
            caloriesPer100g = 165, proteinPer100g = 7.2f, carbsPer100g = 11.0f, fatPer100g = 10.2f,
            defaultUnit = PortionUnit.KATORI, defaultUnitWeightGrams = 150f,
            prepNotes = "Green peas and cottage cheese cubes simmered in spiced onion-tomato curry.", regionOrCategory = "North India"
        ),
        IndianNutritionEntry(
            id = "butter_chicken",
            name = "Butter Chicken (Murgh Makhani)",
            aliases = listOf("butter chicken", "murgh makhani", "बटर चिकन", "মাখন মুরগি"),
            caloriesPer100g = 210, proteinPer100g = 13.5f, carbsPer100g = 8.0f, fatPer100g = 14.5f,
            defaultUnit = PortionUnit.KATORI, defaultUnitWeightGrams = 180f,
            prepNotes = "Tandoori grilled chicken pieces in smooth tomato, butter, and cashew gravy.", regionOrCategory = "Punjabi / North India"
        ),
        IndianNutritionEntry(
            id = "samosa",
            name = "Vegetable Samosa",
            aliases = listOf("samosa", "alu samosa", "samosas", "समोसा", "সিঙাড়া", "சமோசா"),
            caloriesPer100g = 308, proteinPer100g = 4.8f, carbsPer100g = 32.0f, fatPer100g = 18.0f,
            defaultUnit = PortionUnit.PIECE, defaultUnitWeightGrams = 80f,
            prepNotes = "Deep-fried crisp pastry stuffed with spiced potato and pea filling.", regionOrCategory = "Pan-India"
        ),
        IndianNutritionEntry(
            id = "plain_curd_dahi",
            name = "Plain Curd / Dahi",
            aliases = listOf("dahi", "curd", "yogurt", "plain yogurt", "दही", "দই", "தயிர்", "పెరుగు"),
            caloriesPer100g = 60, proteinPer100g = 3.5f, carbsPer100g = 4.7f, fatPer100g = 3.0f,
            defaultUnit = PortionUnit.KATORI, defaultUnitWeightGrams = 120f,
            prepNotes = "Fresh whole milk set curd / dahi.", regionOrCategory = "Pan-India"
        ),
        IndianNutritionEntry(
            id = "pav_bhaji",
            name = "Pav Bhaji",
            aliases = listOf("pav bhaji", "bhaji pav", "mumbai pav bhaji", "पाव भाजी", "পাও ভাজি"),
            caloriesPer100g = 175, proteinPer100g = 4.0f, carbsPer100g = 24.0f, fatPer100g = 7.5f,
            defaultUnit = PortionUnit.PIECE, defaultUnitWeightGrams = 200f,
            prepNotes = "Mashed spiced vegetable curry served with buttered toasted pav bread.", regionOrCategory = "Maharashtra"
        ),
        IndianNutritionEntry(
            id = "chole_bhature",
            name = "Chole Bhature",
            aliases = listOf("chole bhature", "bhatura", "chana bhatura", "छोले भटूरे"),
            caloriesPer100g = 280, proteinPer100g = 7.0f, carbsPer100g = 38.0f, fatPer100g = 12.0f,
            defaultUnit = PortionUnit.PIECE, defaultUnitWeightGrams = 180f,
            prepNotes = "Deep-fried puffed maida bread served with spicy chickpea curry.", regionOrCategory = "Punjab & Delhi"
        ),
        IndianNutritionEntry(
            id = "aloo_paratha",
            name = "Aloo Paratha",
            aliases = listOf("aloo paratha", "paratha", "potato paratha", "आलू पराठा"),
            caloriesPer100g = 240, proteinPer100g = 5.5f, carbsPer100g = 36.0f, fatPer100g = 8.5f,
            defaultUnit = PortionUnit.PIECE, defaultUnitWeightGrams = 110f,
            prepNotes = "Whole wheat flatbread stuffed with spiced mashed potatoes, shallow fried in ghee/butter.", regionOrCategory = "North India"
        ),
        IndianNutritionEntry(
            id = "paneer_paratha",
            name = "Paneer Paratha",
            aliases = listOf("paneer paratha", "cottage cheese paratha", "पनीर पराठा"),
            caloriesPer100g = 265, proteinPer100g = 9.8f, carbsPer100g = 30.0f, fatPer100g = 12.5f,
            defaultUnit = PortionUnit.PIECE, defaultUnitWeightGrams = 110f,
            prepNotes = "Wheat flatbread stuffed with spiced grated paneer.", regionOrCategory = "North India"
        ),
        IndianNutritionEntry(
            id = "upma",
            name = "Rava Upma",
            aliases = listOf("upma", "rava upma", "sooji upma", "उपमा", "உப்புமா", "ఉప్మా"),
            caloriesPer100g = 155, proteinPer100g = 3.8f, carbsPer100g = 26.0f, fatPer100g = 4.2f,
            defaultUnit = PortionUnit.KATORI, defaultUnitWeightGrams = 150f,
            prepNotes = "Roasted semolina cooked with vegetables, mustard seeds, curry leaves, and cashews.", regionOrCategory = "South India"
        ),
        IndianNutritionEntry(
            id = "medu_vada",
            name = "Medu Vada",
            aliases = listOf("vada", "medu vada", "urad vada", "वड़ा", "உளுந்து வடை", "గారెలు"),
            caloriesPer100g = 290, proteinPer100g = 8.2f, carbsPer100g = 28.0f, fatPer100g = 16.5f,
            defaultUnit = PortionUnit.PIECE, defaultUnitWeightGrams = 60f,
            prepNotes = "Crispy deep-fried urad dal fritters served with sambar or coconut chutney.", regionOrCategory = "South India"
        ),
        IndianNutritionEntry(
            id = "dhokla",
            name = "Khaman Dhokla",
            aliases = listOf("dhokla", "khaman", "besan dhokla", "ढोकला", "ઢોકળા"),
            caloriesPer100g = 160, proteinPer100g = 6.5f, carbsPer100g = 23.0f, fatPer100g = 5.0f,
            defaultUnit = PortionUnit.PIECE, defaultUnitWeightGrams = 50f,
            prepNotes = "Steamed fermented gram flour (besan) sponge tempered with mustard seeds and green chili.", regionOrCategory = "Gujarat"
        ),
        IndianNutritionEntry(
            id = "thepla",
            name = "Gujarati Methi Thepla",
            aliases = listOf("thepla", "methi thepla", "થેપલા", "थेपला"),
            caloriesPer100g = 285, proteinPer100g = 8.0f, carbsPer100g = 42.0f, fatPer100g = 9.5f,
            defaultUnit = PortionUnit.PIECE, defaultUnitWeightGrams = 40f,
            prepNotes = "Spiced whole wheat and fenugreek leaf flatbread rolled thin and shallow cooked.", regionOrCategory = "Gujarat"
        ),
        IndianNutritionEntry(
            id = "fish_curry",
            name = "Goan / Bengali Fish Curry",
            aliases = listOf("fish curry", "machhi curry", "fish gravy", "machher jhol", "फिश करी", "মাছের ঝোল", "மீன் குழம்பு"),
            caloriesPer100g = 130, proteinPer100g = 12.5f, carbsPer100g = 4.0f, fatPer100g = 7.2f,
            defaultUnit = PortionUnit.KATORI, defaultUnitWeightGrams = 150f,
            prepNotes = "Fish fillets cooked in mustard or coconut onion-tomato gravy.", regionOrCategory = "Coastal / Bengal & Goa"
        ),
        IndianNutritionEntry(
            id = "chicken_curry",
            name = "Indian Chicken Curry",
            aliases = listOf("chicken curry", "chicken gravy", "murgh curry", "चिकन करी", "চিকেন কারি", "கோழி குழம்பு"),
            caloriesPer100g = 165, proteinPer100g = 14.0f, carbsPer100g = 5.0f, fatPer100g = 9.8f,
            defaultUnit = PortionUnit.KATORI, defaultUnitWeightGrams = 150f,
            prepNotes = "Tender chicken pieces cooked in spiced onion-tomato masala gravy.", regionOrCategory = "Pan-India"
        ),
        IndianNutritionEntry(
            id = "dal_khichdi",
            name = "Dal Khichdi",
            aliases = listOf("khichdi", "dal khichdi", "moong khichdi", "khichuri", "खिचड़ी", "খিচুড়ি"),
            caloriesPer100g = 115, proteinPer100g = 4.2f, carbsPer100g = 20.0f, fatPer100g = 2.2f,
            defaultUnit = PortionUnit.KATORI, defaultUnitWeightGrams = 180f,
            prepNotes = "Wholesome comfort dish of rice and yellow moong lentils simmered with ghee and cumin.", regionOrCategory = "Pan-India"
        ),
        IndianNutritionEntry(
            id = "gulab_jamun",
            name = "Gulab Jamun",
            aliases = listOf("gulab jamun", "jamun", "sweet ball", "गुलाब जामुन", "গোলাপ জামুন"),
            caloriesPer100g = 320, proteinPer100g = 4.0f, carbsPer100g = 52.0f, fatPer100g = 11.0f,
            defaultUnit = PortionUnit.PIECE, defaultUnitWeightGrams = 45f,
            prepNotes = "Deep-fried khoya dumplings soaked in cardamom sugar syrup.", regionOrCategory = "Pan-India"
        ),
        IndianNutritionEntry(
            id = "rasgulla",
            name = "Bengali Rasgulla / Rosogolla",
            aliases = listOf("rasgulla", "rosogolla", "rasagulla", "রসগোল্লা", "रसगुल्ला"),
            caloriesPer100g = 186, proteinPer100g = 4.0f, carbsPer100g = 38.0f, fatPer100g = 1.8f,
            defaultUnit = PortionUnit.PIECE, defaultUnitWeightGrams = 50f,
            prepNotes = "Spongy chhena (cottage cheese) balls boiled in light sugar syrup.", regionOrCategory = "Bengal & Odisha"
        ),
        IndianNutritionEntry(
            id = "jalebi",
            name = "Crispy Jalebi",
            aliases = listOf("jalebi", "zilapi", "जलेबी", "জিলাপি", "ஜிலேபி"),
            caloriesPer100g = 300, proteinPer100g = 2.2f, carbsPer100g = 58.0f, fatPer100g = 7.0f,
            defaultUnit = PortionUnit.PIECE, defaultUnitWeightGrams = 40f,
            prepNotes = "Deep-fried fermented maida batter spirals soaked in saffron sugar syrup.", regionOrCategory = "Pan-India"
        ),
        IndianNutritionEntry(
            id = "pani_puri",
            name = "Pani Puri / Golgappa",
            aliases = listOf("pani puri", "golgappa", "puchka", "पानी पूरी", "গোলগাপ্পা"),
            caloriesPer100g = 190, proteinPer100g = 3.5f, carbsPer100g = 30.0f, fatPer100g = 6.5f,
            defaultUnit = PortionUnit.PIECE, defaultUnitWeightGrams = 20f,
            prepNotes = "Hollow crispy fried puri stuffed with potato, boiled sprouts, and spicy mint water.", regionOrCategory = "Pan-India Street Food"
        ),
        // PACKED FOODS, SNACKS & FITNESS SUPPLEMENTS
        IndianNutritionEntry(
            id = "packed_protein_bar",
            name = "Packed Protein Bar / Snack Bar",
            aliases = listOf("protein bar", "packed protein bar", "nutrition bar", "energy bar", "snack bar", "bar", "প্রোটিন বার", "प्रोटीन बार"),
            caloriesPer100g = 350, proteinPer100g = 33.3f, carbsPer100g = 36.6f, fatPer100g = 11.6f,
            defaultUnit = PortionUnit.PIECE, defaultUnitWeightGrams = 60f,
            prepNotes = "1 packed protein bar (~60g) delivers ~210 kcal and 20g whey/soy protein.", regionOrCategory = "Packed Snack / Fitness"
        ),
        IndianNutritionEntry(
            id = "packed_biscuits",
            name = "Packed Biscuits / Cookies",
            aliases = listOf("biscuits", "cookies", "biscuit pack", "parle g", "good day", "bourbon", "biscuit", "बिस्कुट", "বিস্কুট"),
            caloriesPer100g = 440, proteinPer100g = 7.0f, carbsPer100g = 68.0f, fatPer100g = 18.0f,
            defaultUnit = PortionUnit.PIECE, defaultUnitWeightGrams = 50f,
            prepNotes = "1 small packet of baked wheat/maida biscuits (~50g) contains ~220 kcal.", regionOrCategory = "Packed Snack"
        ),
        IndianNutritionEntry(
            id = "packed_dark_chocolate",
            name = "Packed Dark Chocolate",
            aliases = listOf("dark chocolate", "chocolate", "cadbury", "chocolate bar", "चॉकलेट", "চকলেট"),
            caloriesPer100g = 550, proteinPer100g = 7.3f, carbsPer100g = 56.0f, fatPer100g = 35.0f,
            defaultUnit = PortionUnit.PIECE, defaultUnitWeightGrams = 30f,
            prepNotes = "1 serving of cocoa dark chocolate (~30g) provides ~165 kcal.", regionOrCategory = "Packed Confectionery"
        ),
        IndianNutritionEntry(
            id = "packed_chips",
            name = "Packed Potato Chips / Namkeen",
            aliases = listOf("chips", "potato chips", "lays", "kurkure", "namkeen", "packaged snack", "चिप्स", "চিপস"),
            caloriesPer100g = 530, proteinPer100g = 6.6f, carbsPer100g = 50.0f, fatPer100g = 35.0f,
            defaultUnit = PortionUnit.PIECE, defaultUnitWeightGrams = 30f,
            prepNotes = "1 small pack of fried potato chips or namkeen (~30g) contains ~160 kcal.", regionOrCategory = "Packed Snack"
        ),
        IndianNutritionEntry(
            id = "creatine_monohydrate",
            name = "Creatine Monohydrate (Supplement)",
            aliases = listOf("creatine", "creatine monohydrate", "creatine powder", "creatine scoop", "क्रिएटिन"),
            caloriesPer100g = 0, proteinPer100g = 0.0f, carbsPer100g = 0.0f, fatPer100g = 0.0f,
            defaultUnit = PortionUnit.PIECE, defaultUnitWeightGrams = 5f,
            prepNotes = "Pure Unflavored Creatine Monohydrate (1 scoop = 3g - 5g). Boosts ATP muscle strength and cell hydration with ZERO calories.", regionOrCategory = "Fitness Supplement"
        ),
        IndianNutritionEntry(
            id = "whey_protein",
            name = "Whey Protein Powder (1 Scoop)",
            aliases = listOf("whey", "protein powder", "whey protein", "protein shake", "whey isolate", "वे प्रोटीन"),
            caloriesPer100g = 400, proteinPer100g = 80.0f, carbsPer100g = 6.0f, fatPer100g = 5.0f,
            defaultUnit = PortionUnit.PIECE, defaultUnitWeightGrams = 30f,
            prepNotes = "1 scoop (~30g) provides ~120 kcal and 24g fast-absorbing protein for muscle recovery.", regionOrCategory = "Fitness Supplement"
        )
    )

    val items: List<IndianNutritionEntry>
        get() = defaultItems + customEntries

    fun addCustomEntry(entry: IndianNutritionEntry) {
        customEntries.add(entry)
    }

    fun findMatchingEntry(query: String): IndianNutritionEntry {
        val q = query.trim().lowercase()
        return items.find { entry ->
            entry.name.lowercase().contains(q) || entry.aliases.any { alias -> alias.lowercase() == q || q.contains(alias) }
        } ?: items.first()
    }

    fun searchEntries(query: String): List<IndianNutritionEntry> {
        if (query.isBlank()) return items
        val q = query.trim().lowercase()
        return items.filter { entry ->
            entry.name.lowercase().contains(q) || entry.aliases.any { it.lowercase().contains(q) }
        }
    }

    fun createEditableFoodItem(entry: IndianNutritionEntry, quantity: Float = 1.0f): EditableFoodItem {
        return EditableFoodItem(
            id = UUID.randomUUID().toString(),
            foodName = entry.name,
            confidenceScore = 0.88f,
            quantity = quantity,
            unit = entry.defaultUnit,
            prepStyle = PrepStyle.STANDARD,
            caloriesPer100g = entry.caloriesPer100g,
            proteinPer100g = entry.proteinPer100g,
            carbsPer100g = entry.carbsPer100g,
            fatPer100g = entry.fatPer100g,
            preparationNote = entry.prepNotes
        )
    }
}

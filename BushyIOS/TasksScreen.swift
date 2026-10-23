import SwiftUI

public struct TasksScreen: View {
    @ObservedObject var viewModel: HealthViewModel
    var onAddQuest: () -> Void
    var onScanMeal: () -> Void
    
    public init(
        viewModel: HealthViewModel,
        onAddQuest: @escaping () -> Void,
        onScanMeal: @escaping () -> Void
    ) {
        self.viewModel = viewModel
        self.onAddQuest = onAddQuest
        self.onScanMeal = onScanMeal
    }
    
    public var body: some View {
        ZStack {
            // Ambient Liquid Background (Pure Monochrome)
            LiquidFluidBackground(isMonochrome: true)
            
            ScrollView(showsIndicators: false) {
                VStack(spacing: 24) {
                    // Header
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Fitness Quests 🎯")
                                .font(.title2)
                                .fontWeight(.black)
                            Text("Complete quests to earn XP and level up Bloub")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                        Button(action: onAddQuest) {
                            Image(systemName: "plus")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(Color.bushyAppBackground)
                                .frame(width: 44, height: 44)
                                .background(Color.primary)
                                .clipShape(Circle())
                                .overlay(
                                    Circle().stroke(Color.white.opacity(0.35), lineWidth: 1.2)
                                )
                                .shadow(color: Color.black.opacity(0.2), radius: 8, x: 0, y: 4)
                        }
                    }
                    
                    // Tasks List
                    VStack(spacing: 14) {
                        ForEach(viewModel.uiState.tasks) { task in
                            taskCard(task)
                        }
                    }
                    
                    // Macro Nutrition Dashboard
                    macroNutritionSection
                    
                    // Logged Meals History
                    loggedMealsSection
                    
                    Spacer(minLength: 120) // Spacing for bottom floating pill
                }
                .padding(.horizontal, 20)
                .padding(.top, 16)
            }
        }
    }
    
    // MARK: - Task Card
    
    private func taskCard(_ task: HealthTask) -> some View {
        VStack(spacing: 12) {
            HStack(spacing: 14) {
                Image(systemName: task.type.iconName)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.primary)
                    .frame(width: 44, height: 44)
                    .liquidGlassCircle()
                
                VStack(alignment: .leading, spacing: 4) {
                    Text(task.title)
                        .font(.headline)
                        .fontWeight(.bold)
                    
                    Text("\(task.current) / \(task.target) \(task.type.unitLabel)")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                if task.isCompleted {
                    HStack(spacing: 4) {
                        Image(systemName: "checkmark.seal.fill")
                            .foregroundColor(.primary)
                        Text("+500 XP")
                            .font(.caption)
                            .fontWeight(.bold)
                            .foregroundColor(.primary)
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .liquidGlassPill()
                } else {
                    Button {
                        HapticManager.impact(style: .medium)
                        viewModel.singleTapIncrement(task.id)
                    } label: {
                        HStack(spacing: 4) {
                            Image(systemName: "plus")
                            Text("1")
                        }
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(Color.bushyAppBackground)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                        .background(Color.primary)
                        .clipShape(Capsule())
                        .overlay(
                            Capsule().stroke(Color.white.opacity(0.35), lineWidth: 1)
                        )
                        .shadow(color: Color.clear, radius: 6, x: 0, y: 3)
                    }
                }
            }
            
            // Progress Bar
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(Color.secondary.opacity(0.18))
                        .frame(height: 6)
                    
                    Capsule()
                        .fill(Color.primary)
                        .frame(width: max(0, geo.size.width * CGFloat(task.progress)), height: 6)
                }
            }
            .frame(height: 6)
            
            // Actions Row (Reset, Delete)
            HStack {
                Spacer()
                Button {
                    viewModel.resetTask(task.id)
                } label: {
                    Text("Reset")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
                
                Text("•").font(.caption2).foregroundColor(.secondary)
                
                Button {
                    viewModel.deleteTask(task.id)
                } label: {
                    Text("Delete")
                        .font(.caption2)
                        .foregroundColor(.red.opacity(0.8))
                }
            }
        }
        .padding(16)
        .liquidGlassCard(cornerRadius: 22)
    }
    
    // MARK: - Macro Nutrition Section
    
    private var macroNutritionSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text("Today's Nutrition 🥑")
                    .font(.title3)
                    .fontWeight(.bold)
                Spacer()
                Button(action: onScanMeal) {
                    Text("Log Meal")
                        .font(.subheadline)
                        .fontWeight(.bold)
                        .foregroundColor(.primary)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .liquidGlassPill()
                }
            }
            
            HStack(spacing: 12) {
                macroCard(label: "Protein", amount: "\(viewModel.uiState.totalProteinGrams)g", target: "120g")
                macroCard(label: "Carbs", amount: "\(viewModel.uiState.totalCarbsGrams)g", target: "200g")
                macroCard(label: "Fats", amount: "\(viewModel.uiState.totalFatGrams)g", target: "60g")
            }
        }
        .padding(.top, 8)
    }
    
    private func macroCard(label: String, amount: String, target: String) -> some View {
        VStack(spacing: 6) {
            Text(label)
                .font(.caption)
                .fontWeight(.bold)
                .foregroundColor(.secondary)
            
            Text(amount)
                .font(.system(size: 20, weight: .black, design: .rounded))
                .foregroundColor(.primary)
            
            Text("Goal: \(target)")
                .font(.caption2)
                .foregroundColor(.secondary)
        }
        .padding(14)
        .frame(maxWidth: .infinity)
        .liquidGlassCard(cornerRadius: 18)
    }
    
    // MARK: - Logged Meals Section
    
    private var loggedMealsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Logged Meals")
                .font(.headline)
                .fontWeight(.bold)
            
            if viewModel.uiState.loggedMeals.isEmpty {
                VStack(spacing: 8) {
                    Image(systemName: "fork.knife")
                        .font(.system(size: 28))
                        .foregroundColor(.secondary)
                    Text("No meals logged today yet.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 28)
                .liquidGlassCard(cornerRadius: 20)
            } else {
                ForEach(viewModel.uiState.loggedMeals) { meal in
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(meal.name)
                                .font(.subheadline)
                                .fontWeight(.bold)
                            Text("\(meal.calories) kcal • \(meal.proteinGrams)g P • \(meal.carbsGrams)g C • \(meal.fatGrams)g F")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        
                        Spacer()
                        
                        Button {
                            viewModel.deleteMeal(meal.id)
                        } label: {
                            Image(systemName: "trash")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding(16)
                    .liquidGlassCard(cornerRadius: 18)
                }
            }
        }
    }
}

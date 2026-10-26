import SwiftUI

public struct HealthScreen: View {
    @ObservedObject var viewModel: HealthViewModel
    @StateObject private var aiViewModel = BushyAIViewModel()
    
    @State private var selectedTab: Int = 0
    @State private var showAddTaskSheet: Bool = false
    @State private var showScanMealSheet: Bool = false
    @State private var showShareSheet: Bool = false
    @State private var showSwitchHeroSheet: Bool = false
    
    public init(viewModel: HealthViewModel) {
        self.viewModel = viewModel
    }
    
    public var body: some View {
        ZStack(alignment: .bottom) {
            // Main Tab View
            TabView(selection: $selectedTab) {
                HomeScreen(
                    viewModel: viewModel,
                    onOpenQuests: {
                        withAnimation { selectedTab = 1 }
                    },
                    onShare: {
                        showShareSheet = true
                    },
                    onSwitchHero: {
                        showSwitchHeroSheet = true
                    }
                )
                .tag(0)
                
                TasksScreen(
                    viewModel: viewModel,
                    onAddQuest: {
                        showAddTaskSheet = true
                    },
                    onScanMeal: {
                        showScanMealSheet = true
                    }
                )
                .tag(1)
                
                BushyAIScreen(
                    aiViewModel: aiViewModel,
                    healthViewModel: viewModel,
                    onScanMeal: {
                        showScanMealSheet = true
                    }
                )
                .tag(2)
            }
            #if os(iOS)
            .tabViewStyle(.page(indexDisplayMode: .never))
            #endif
            .ignoresSafeArea(edges: .bottom)
            
            // Dynamic Floating Navigation Pill & Anchored Actions Menu
            NavigationPill(
                selectedTab: $selectedTab,
                visualStyle: viewModel.uiState.visualStyle,
                onAddQuest: {
                    showAddTaskSheet = true
                },
                onScanMeal: {
                    showScanMealSheet = true
                },
                onShareStory: {
                    showShareSheet = true
                }
            )
        }
        .sheet(isPresented: $showAddTaskSheet) {
            AddTaskSheet(viewModel: viewModel)
        }
        .sheet(isPresented: $showScanMealSheet) {
            MealAnalysisScreen(healthViewModel: viewModel, aiViewModel: aiViewModel)
        }
        .sheet(isPresented: $showShareSheet) {
            StoryShare(viewModel: viewModel)
        }
        .sheet(isPresented: $showSwitchHeroSheet) {
            SwitchHeroSheet(viewModel: viewModel)
        }
    }
}

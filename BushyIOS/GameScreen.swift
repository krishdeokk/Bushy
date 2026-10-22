import SwiftUI

public struct GameScreen: View {
    @ObservedObject var viewModel: GameViewModel
    @StateObject private var aiViewModel = BushyAIViewModel()
    
    @State private var selectedTab: Int = 0
    @State private var showAddTaskSheet: Bool = false
    @State private var showScanMealSheet: Bool = false
    @State private var showShareSheet: Bool = false
    @State private var showSwitchHeroSheet: Bool = false
    
    public init(viewModel: GameViewModel) {
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
                    gameViewModel: viewModel,
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
            
            // Dynamic Floating Navigation Pill
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
            .padding(.bottom, 16)
        }
        .sheet(isPresented: $showAddTaskSheet) {
            AddTaskSheet(viewModel: viewModel)
        }
        .sheet(isPresented: $showScanMealSheet) {
            MealAnalysisView(gameViewModel: viewModel, aiViewModel: aiViewModel)
        }
        .sheet(isPresented: $showShareSheet) {
            StoryShareView(viewModel: viewModel)
        }
        .sheet(isPresented: $showSwitchHeroSheet) {
            SwitchHeroSheet(viewModel: viewModel)
        }
    }
}

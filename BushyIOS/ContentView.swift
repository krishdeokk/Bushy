import SwiftUI

struct ContentView: View {
    @StateObject private var viewModel = HealthViewModel()
    
    var body: some View {
        Group {
            if !viewModel.uiState.isSetupComplete {
                SetupScreen(viewModel: viewModel)
            } else {
                HealthScreen(viewModel: viewModel)
            }
        }
        .tint(.primary)
        .preferredColorScheme(colorScheme)
    }
    
    private var colorScheme: ColorScheme? {
        switch viewModel.uiState.themeMode {
        case .light: return .light
        case .dark: return .dark
        case .system: return nil
        }
    }
}

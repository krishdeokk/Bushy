import SwiftUI

struct ContentView: View {
    @StateObject private var viewModel = GameViewModel()
    
    var body: some View {
        Group {
            if !viewModel.uiState.isSetupComplete {
                SetupScreen(viewModel: viewModel)
            } else {
                GameScreen(viewModel: viewModel)
            }
        }
        .tint(viewModel.uiState.visualStyle == .monochrome ? .primary : Color.accentColor)
        .grayscale(viewModel.uiState.visualStyle == .monochrome ? 1.0 : 0.0)
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

#Preview {
    ContentView()
}

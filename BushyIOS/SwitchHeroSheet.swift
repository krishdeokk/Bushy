import SwiftUI

public struct SwitchHeroSheet: View {
    @ObservedObject var viewModel: GameViewModel
    @Environment(\.dismiss) private var dismiss
    
    public var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                Text("Select Your Bloub Avatar")
                    .font(.headline)
                    .foregroundColor(.secondary)
                    .padding(.top, 16)
                
                HStack(spacing: 20) {
                    ForEach(AvatarType.allCases) { type in
                        Button {
                            viewModel.setAvatar(type)
                            dismiss()
                        } label: {
                            VStack(spacing: 12) {
                                BloubAvatarView(
                                    expression: .happy,
                                    visualStyle: viewModel.uiState.visualStyle,
                                    themeMode: viewModel.uiState.themeMode
                                )
                                .frame(width: 120, height: 120)
                                
                                Text(type.displayName)
                                    .fontWeight(.bold)
                                    .foregroundColor(viewModel.uiState.avatarType == type ? .accentColor : .primary)
                            }
                            .padding()
                            .frame(width: 140, height: 180)
                            .background(viewModel.uiState.avatarType == type ? Color.accentColor.opacity(0.12) : Color.secondary.opacity(0.08))
                            .clipShape(RoundedRectangle(cornerRadius: 24))
                            .overlay(
                                RoundedRectangle(cornerRadius: 24)
                                    .stroke(viewModel.uiState.avatarType == type ? Color.accentColor : Color.clear, lineWidth: 2)
                            )
                        }
                    }
                }
                
                Spacer()
            }
            .navigationTitle("Switch Hero")
            #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}

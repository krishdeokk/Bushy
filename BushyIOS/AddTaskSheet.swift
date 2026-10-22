import SwiftUI

public struct AddTaskSheet: View {
    @ObservedObject var viewModel: GameViewModel
    @Environment(\.dismiss) private var dismiss
    
    @State private var title: String = ""
    @State private var targetText: String = "20"
    @State private var selectedType: TaskType = .pushups
    
    public var body: some View {
        NavigationStack {
            Form {
                Section(header: Text("Mission Details")) {
                    TextField("Mission Title (e.g. Morning Pushups)", text: $title)
                    
                    Picker("Quest Type", selection: $selectedType) {
                        ForEach(TaskType.allCases, id: \.self) { type in
                            HStack {
                                Image(systemName: type.iconName)
                                Text(type.rawValue.capitalized)
                            }
                            .tag(type)
                        }
                    }
                    
                    HStack {
                        Text("Target (\(selectedType.unitLabel))")
                        Spacer()
                        TextField("20", text: $targetText)
                            #if os(iOS)
                            .keyboardType(.numberPad)
                            #endif
                            .multilineTextAlignment(.trailing)
                    }
                }
                
                Section {
                    Button {
                        let target = Int(targetText) ?? 20
                        let finalTitle = title.isEmpty ? "Daily \(selectedType.rawValue.capitalized)" : title
                        viewModel.addNewTask(title: finalTitle, target: target, type: selectedType)
                        dismiss()
                    } label: {
                        HStack {
                            Spacer()
                            Text("Deploy Quest 🎯")
                                .fontWeight(.bold)
                                .foregroundColor(.accentColor)
                            Spacer()
                        }
                    }
                }
            }
            .navigationTitle("New Quest")
            #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }
}

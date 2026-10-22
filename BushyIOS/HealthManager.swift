import Foundation
import HealthKit

public final class HealthManager: @unchecked Sendable {
    public static let shared = HealthManager()
    
    private let healthStore: HKHealthStore?
    
    private init() {
        if HKHealthStore.isHealthDataAvailable() {
            self.healthStore = HKHealthStore()
        } else {
            self.healthStore = nil
        }
    }
    
    public var isAvailable: Bool {
        healthStore != nil
    }
    
    public func requestPermissions() async -> Bool {
        guard let healthStore = healthStore else { return false }
        guard let stepType = HKQuantityType.quantityType(forIdentifier: .stepCount) else { return false }
        
        do {
            try await healthStore.requestAuthorization(toShare: [], read: [stepType])
            return true
        } catch {
            print("HealthKit permission error: \(error.localizedDescription)")
            return false
        }
    }
    
    public func fetchTodaySteps() async -> Int {
        guard let healthStore = healthStore else { return 0 }
        guard let stepType = HKQuantityType.quantityType(forIdentifier: .stepCount) else { return 0 }
        
        let calendar = Calendar.current
        let now = Date()
        let startOfDay = calendar.startOfDay(for: now)
        let predicate = HKQuery.predicateForSamples(withStart: startOfDay, end: now, options: .strictStartDate)
        
        return await withCheckedContinuation { continuation in
            let query = HKStatisticsQuery(
                quantityType: stepType,
                quantitySamplePredicate: predicate,
                options: .cumulativeSum
            ) { _, result, error in
                guard let result = result, let sum = result.sumQuantity() else {
                    continuation.resume(returning: 0)
                    return
                }
                let steps = Int(sum.doubleValue(for: HKUnit.count()))
                continuation.resume(returning: steps)
            }
            healthStore.execute(query)
        }
    }
}

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
        guard let stepType = HKQuantityType.quantityType(forIdentifier: .stepCount),
              let energyType = HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned) else {
            return false
        }
        
        do {
            try await healthStore.requestAuthorization(toShare: [], read: [stepType, energyType])
            return true
        } catch {
            print("HealthKit permission error: \(error.localizedDescription)")
            return false
        }
    }
    
    public func requestAuthorization(completion: @escaping (Bool, Error?) -> Void) {
        Task {
            let success = await requestPermissions()
            DispatchQueue.main.async {
                completion(success, nil)
            }
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
    
    public func fetchTodaySteps(completion: @escaping (Int) -> Void) {
        Task {
            let steps = await fetchTodaySteps()
            DispatchQueue.main.async {
                completion(steps)
            }
        }
    }
    
    public func fetchTodayActiveCalories() async -> Int {
        guard let healthStore = healthStore else { return 0 }
        guard let energyType = HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned) else { return 0 }
        
        let calendar = Calendar.current
        let now = Date()
        let startOfDay = calendar.startOfDay(for: now)
        let predicate = HKQuery.predicateForSamples(withStart: startOfDay, end: now, options: .strictStartDate)
        
        return await withCheckedContinuation { continuation in
            let query = HKStatisticsQuery(
                quantityType: energyType,
                quantitySamplePredicate: predicate,
                options: .cumulativeSum
            ) { _, result, error in
                guard let result = result, let sum = result.sumQuantity() else {
                    continuation.resume(returning: 0)
                    return
                }
                let cals = Int(sum.doubleValue(for: HKUnit.kilocalorie()))
                continuation.resume(returning: cals)
            }
            healthStore.execute(query)
        }
    }
    
    public func fetchTodayActiveCalories(completion: @escaping (Int) -> Void) {
        Task {
            let cal = await fetchTodayActiveCalories()
            DispatchQueue.main.async {
                completion(cal)
            }
        }
    }
}

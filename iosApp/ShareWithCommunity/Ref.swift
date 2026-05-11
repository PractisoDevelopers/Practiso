struct Ref<T: AnyObject>: Equatable, Hashable {
    let value: T

    static func == (lhs: Ref<T>, rhs: Ref<T>) -> Bool {
        lhs.value === rhs.value
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(ObjectIdentifier(value))
    }
}

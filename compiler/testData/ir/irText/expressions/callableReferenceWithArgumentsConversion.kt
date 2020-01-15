// !LANGUAGE: +NewInference, +FunctionReferenceWithDefaultValueAsOtherType

fun use(fn: (Int) -> Int) = fn(1)

fun fnWithDefault(a: Int, b: Int = 42) = 0

fun fnWithVarargs(vararg xs: Int) = 0

fun testDefault() = use(::fnWithDefault)

fun testVararg() = use(::fnWithVarargs)
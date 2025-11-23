package dev.sadakat.qit.wear.application.usecase

/**
 * Base class for use cases with no input
 */
abstract class BaseUseCase<out Type> {
    abstract suspend operator fun invoke(): Result<Type>
}

/**
 * Base class for use cases with single input
 */
abstract class UseCaseWithParams<in Params, out Type> {
    abstract suspend operator fun invoke(params: Params): Result<Type>
}

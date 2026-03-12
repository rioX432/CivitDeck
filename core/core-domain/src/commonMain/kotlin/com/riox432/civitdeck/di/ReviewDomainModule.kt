package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.GetModelReviewsUseCase
import com.riox432.civitdeck.domain.usecase.GetRatingTotalsUseCase
import com.riox432.civitdeck.domain.usecase.SubmitReviewUseCase
import org.koin.dsl.module

val reviewDomainModule = module {
    factory { GetModelReviewsUseCase(get()) }
    factory { GetRatingTotalsUseCase(get()) }
    factory { SubmitReviewUseCase(get()) }
}

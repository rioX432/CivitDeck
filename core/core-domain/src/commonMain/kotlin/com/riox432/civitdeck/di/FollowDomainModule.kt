package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.FollowCreatorUseCase
import com.riox432.civitdeck.domain.usecase.GetCreatorFeedUseCase
import com.riox432.civitdeck.domain.usecase.GetFollowedCreatorsUseCase
import com.riox432.civitdeck.domain.usecase.GetUnreadFeedCountUseCase
import com.riox432.civitdeck.domain.usecase.IsFollowingCreatorUseCase
import com.riox432.civitdeck.domain.usecase.MarkFeedReadUseCase
import com.riox432.civitdeck.domain.usecase.UnfollowCreatorUseCase
import org.koin.dsl.module

val followDomainModule = module {
    factory { FollowCreatorUseCase(get()) }
    factory { UnfollowCreatorUseCase(get()) }
    factory { IsFollowingCreatorUseCase(get()) }
    factory { GetCreatorFeedUseCase(get()) }
    factory { GetUnreadFeedCountUseCase(get()) }
    factory { MarkFeedReadUseCase(get()) }
    factory { GetFollowedCreatorsUseCase(get()) }
}

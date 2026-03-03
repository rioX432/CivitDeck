package com.riox432.civitdeck.di

import org.koin.dsl.module

val domainModule = module {
    includes(
        detailDomainModule,
        favoritesDomainModule,
        nsfwDomainModule,
        historyDomainModule,
        settingsDomainModule,
        authDomainModule,
        localModelsDomainModule,
        themeDomainModule,
        notificationsDomainModule,
        cacheDomainModule,
        tutorialDomainModule,
        civitaiLinkDomainModule,
        datasetDomainModule,
    )
}

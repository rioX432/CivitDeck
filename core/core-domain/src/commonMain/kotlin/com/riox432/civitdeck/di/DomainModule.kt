package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.util.ApplicationScope
import org.koin.dsl.module

val domainModule = module {
    single { ApplicationScope() }
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
        notesDomainModule,
        analyticsDomainModule,
        followDomainModule,
        downloadDomainModule,
        reviewDomainModule,
        backupDomainModule,
        pluginDomainModule,
        shareDomainModule,
        updateDomainModule,
        embeddingDomainModule,
        collectionsDomainModule,
        galleryDomainModule,
        promptsDomainModule,
        searchDomainModule,
    )
}

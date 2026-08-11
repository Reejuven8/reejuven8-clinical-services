package com.reejuven8.ninemo.shared.di

import com.reejuven8.ninemo.shared.network.nineMoHttpClient
import com.reejuven8.ninemo.shared.repository.AbhaRepository
import com.reejuven8.ninemo.shared.repository.AuthRepository
import com.reejuven8.ninemo.shared.repository.ChatSocketClient
import com.reejuven8.ninemo.shared.repository.CommunityRepository
import com.reejuven8.ninemo.shared.repository.ConsentRepository
import com.reejuven8.ninemo.shared.repository.ContentRepository
import com.reejuven8.ninemo.shared.repository.ContractionRepository
import com.reejuven8.ninemo.shared.repository.DietRepository
import com.reejuven8.ninemo.shared.repository.FilesRepository
import com.reejuven8.ninemo.shared.repository.GrowthRepository
import com.reejuven8.ninemo.shared.repository.HealthRecordsRepository
import com.reejuven8.ninemo.shared.repository.KickCounterRepository
import com.reejuven8.ninemo.shared.repository.MilestoneRepository
import com.reejuven8.ninemo.shared.repository.ModeTransitionRepository
import com.reejuven8.ninemo.shared.repository.PregnancyProfileRepository
import com.reejuven8.ninemo.shared.repository.SummaryCardRepository
import com.reejuven8.ninemo.shared.repository.SymptomRepository
import com.reejuven8.ninemo.shared.repository.TimelineRepository
import com.reejuven8.ninemo.shared.repository.VaccinationRepository
import com.reejuven8.ninemo.shared.repository.VitalsRepository
import com.reejuven8.ninemo.shared.viewmodel.AbhaLinkViewModel
import com.reejuven8.ninemo.shared.viewmodel.AuthViewModel
import com.reejuven8.ninemo.shared.viewmodel.ChatViewModel
import com.reejuven8.ninemo.shared.viewmodel.ChildDashboardViewModel
import com.reejuven8.ninemo.shared.viewmodel.CommunityViewModel
import com.reejuven8.ninemo.shared.viewmodel.ConsentViewModel
import com.reejuven8.ninemo.shared.viewmodel.ContentViewModel
import com.reejuven8.ninemo.shared.viewmodel.ContractionViewModel
import com.reejuven8.ninemo.shared.viewmodel.DietViewModel
import com.reejuven8.ninemo.shared.viewmodel.DocumentDetailViewModel
import com.reejuven8.ninemo.shared.viewmodel.GrowthViewModel
import com.reejuven8.ninemo.shared.viewmodel.HealthLockerViewModel
import com.reejuven8.ninemo.shared.viewmodel.KickCounterViewModel
import com.reejuven8.ninemo.shared.viewmodel.MilestoneViewModel
import com.reejuven8.ninemo.shared.viewmodel.ModeTransitionViewModel
import com.reejuven8.ninemo.shared.viewmodel.OnboardingViewModel
import com.reejuven8.ninemo.shared.viewmodel.ProfileViewModel
import com.reejuven8.ninemo.shared.viewmodel.RegisterViewModel
import com.reejuven8.ninemo.shared.viewmodel.SessionViewModel
import com.reejuven8.ninemo.shared.viewmodel.SummaryCardViewModel
import com.reejuven8.ninemo.shared.viewmodel.SymptomLogViewModel
import com.reejuven8.ninemo.shared.viewmodel.TimelineViewModel
import com.reejuven8.ninemo.shared.viewmodel.VaccinationViewModel
import com.reejuven8.ninemo.shared.viewmodel.VitalsViewModel
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/** Platform module supplies the Ktor engine, SessionStore, and PlatformConfig. */
expect val platformModule: Module

/**
 * Shared graph: HTTP client + session gate + F1 auth/onboarding vertical.
 * Repositories and feature ViewModels for later phases register here as they land.
 */
val sharedModule: Module = module {
    single { nineMoHttpClient(get(), get()) }
    viewModelOf(::SessionViewModel)

    // F1 — Auth + onboarding (P0-P4)
    single { AuthRepository(get(), get()) }
    single { AbhaRepository(get(), get()) }
    single { PregnancyProfileRepository(get()) }
    viewModelOf(::AuthViewModel)
    viewModelOf(::RegisterViewModel)
    viewModelOf(::AbhaLinkViewModel)
    viewModelOf(::OnboardingViewModel)

    // F2 — Pregnancy home + core tools (P5, P10, P11, P12, P16)
    single { TimelineRepository(get()) }
    single { SymptomRepository(get()) }
    single { VitalsRepository(get()) }
    single { SummaryCardRepository(get()) }
    viewModelOf(::TimelineViewModel)
    viewModelOf(::SymptomLogViewModel)
    viewModelOf(::VitalsViewModel)
    viewModelOf(::SummaryCardViewModel)

    // F3 — T3 tools + diet (P13, P14, P15)
    single { KickCounterRepository(get()) }
    single { ContractionRepository(get()) }
    single { DietRepository(get()) }
    viewModelOf(::KickCounterViewModel)
    viewModelOf(::ContractionViewModel)
    viewModelOf(::DietViewModel)

    // F4 — Health Locker + consent (P7, P8, P9)
    single { HealthRecordsRepository(get()) }
    single { FilesRepository(get()) }
    single { ConsentRepository(get()) }
    viewModelOf(::HealthLockerViewModel)
    viewModelOf(::DocumentDetailViewModel)
    viewModelOf(::ConsentViewModel)

    // F5 — Child mode (P6, P17, P18, P19, P23)
    single { GrowthRepository(get()) }
    single { VaccinationRepository(get()) }
    single { MilestoneRepository(get()) }
    single { ModeTransitionRepository(get()) }
    viewModelOf(::ChildDashboardViewModel)
    viewModelOf(::GrowthViewModel)
    viewModelOf(::VaccinationViewModel)
    viewModelOf(::MilestoneViewModel)
    viewModelOf(::ModeTransitionViewModel)

    // F6 — Community (P20 chat), content feed (P21), profile (P22)
    single { CommunityRepository(get()) }
    single { ContentRepository(get()) }
    // factory: each open channel screen gets its own STOMP connection lifecycle.
    factory { ChatSocketClient(get()) }
    viewModelOf(::CommunityViewModel)
    viewModelOf(::ChatViewModel)
    viewModelOf(::ContentViewModel)
    viewModelOf(::ProfileViewModel)
}

/** Android calls this from Application; iOS calls doInitKoin() from @main. */
fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication =
    startKoin {
        appDeclaration()
        modules(platformModule, sharedModule)
    }

/** Convenience entry point for iOS (no default-arg support across the Obj-C bridge). */
fun doInitKoin() = initKoin()

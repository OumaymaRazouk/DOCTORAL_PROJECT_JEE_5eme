package com.exemple.soutenancestatgrpcservice.grpc;

import com.exemple.soutenancestatgrpcservice.model.SoutenanceView;
import com.exemple.soutenancestatgrpcservice.model.WeeklySoutenanceSummary;
import com.exemple.soutenancestatgrpcservice.service.SoutenanceStatsService;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;

@GrpcService
public class SoutenanceStatGrpcService extends SoutenanceStatServiceGrpc.SoutenanceStatServiceImplBase {

    private final SoutenanceStatsService statsService;

    public SoutenanceStatGrpcService(SoutenanceStatsService statsService) {
        this.statsService = statsService;
    }

    @Override
    public void getWeeklySoutenanceStats(WeeklyStatsRequest request, StreamObserver<WeeklyStatsResponse> responseObserver) {
        List<WeeklySoutenanceSummary> summaries = statsService.getWeeklyStats(
                request.getStartDate().isEmpty() ? null : request.getStartDate(),
                request.getEndDate().isEmpty() ? null : request.getEndDate(),
                request.getIncludeDetails()
        );
        WeeklyStatsResponse.Builder builder = WeeklyStatsResponse.newBuilder();
        for (WeeklySoutenanceSummary s : summaries) {
            builder.addWeeks(toWeekMessage(s, request.getIncludeDetails()));
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void streamWeeklySoutenanceStats(WeeklyStatsRequest request, StreamObserver<WeeklyWeekStats> responseObserver) {
        List<WeeklySoutenanceSummary> summaries = statsService.getWeeklyStats(
                request.getStartDate().isEmpty() ? null : request.getStartDate(),
                request.getEndDate().isEmpty() ? null : request.getEndDate(),
                request.getIncludeDetails()
        );
        for (WeeklySoutenanceSummary s : summaries) {
            responseObserver.onNext(toWeekMessage(s, request.getIncludeDetails()));
        }
        responseObserver.onCompleted();
    }

    private WeeklyWeekStats toWeekMessage(WeeklySoutenanceSummary s, boolean includeDetails) {
        WeeklyWeekStats.Builder b = WeeklyWeekStats.newBuilder()
                .setWeekStart(s.getWeekStart())
                .setWeekEnd(s.getWeekEnd())
                .setTotal(s.getTotal());
        if (includeDetails && s.getSoutenances() != null) {
            for (SoutenanceView v : s.getSoutenances()) {
                b.addSoutenances(toDetail(v));
            }
        }
        return b.build();
    }

    private SoutenanceDetail toDetail(SoutenanceView v) {
        SoutenanceDetail.Builder b = SoutenanceDetail.newBuilder()
                .setId(v.getId())
                .setDoctorantId(v.getDoctorantId() == null ? "" : v.getDoctorantId())
                .setDirecteurId(v.getDirecteurId() == null ? "" : v.getDirecteurId())
                .setStatus(v.getStatus() == null ? "" : v.getStatus())
                .setTitreTheseDefinitif(v.getTitreTheseDefinitif() == null ? "" : v.getTitreTheseDefinitif())
                .setDatePlanifiee(v.getDatePlanifiee() == null ? "" : v.getDatePlanifiee())
                .setSalle(v.getSalle() == null ? "" : v.getSalle())
                .setLieu(v.getLieu() == null ? "" : v.getLieu());
        if (v.getJury() != null) {
            v.getJury().forEach(j -> b.addJury(JuryMemberDetail.newBuilder()
                    .setNom(j.getNom() == null ? "" : j.getNom())
                    .setPrenom(j.getPrenom() == null ? "" : j.getPrenom())
                    .setAffiliation(j.getAffiliation() == null ? "" : j.getAffiliation())
                    .setRole(j.getRole() == null ? "" : j.getRole())
                    .build()));
        }
        return b.build();
    }
}

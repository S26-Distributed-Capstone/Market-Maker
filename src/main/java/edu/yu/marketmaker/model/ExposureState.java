package edu.yu.marketmaker.model;

public record ExposureState(long currentUsage, long totalCapacity, int activeReservations) {}
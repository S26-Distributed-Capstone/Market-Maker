package edu.yu.marketmaker.model;

public record ExposureState(int currentUsage, int totalCapacity, int activeReservations) {}
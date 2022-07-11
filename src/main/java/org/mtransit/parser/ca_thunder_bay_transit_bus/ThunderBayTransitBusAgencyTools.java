package org.mtransit.parser.ca_thunder_bay_transit_bus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CharUtils;
import org.mtransit.commons.CleanUtils;
import org.mtransit.parser.ColorUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;

import java.util.List;
import java.util.Locale;

// http://www.thunderbay.ca/Living/Getting_Around/Thunder_Bay_Transit/Developers_-_Open_Data.htm
public class ThunderBayTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new ThunderBayTransitBusAgencyTools().start(args);
	}

	@Nullable
	@Override
	public List<Locale> getSupportedLanguages() {
		return LANG_EN;
	}

	@Override
	public boolean defaultExcludeEnabled() {
		return true;
	}

	@NotNull
	@Override
	public String getAgencyName() {
		return "Thunder Bay Transit";
	}

	@Override
	public boolean excludeRoute(@NotNull GRoute gRoute) {
		final String rlnLC = gRoute.getRouteLongNameOrDefault().toLowerCase(getFirstLanguageNN());
		if (rlnLC.contains("test")) {
			return EXCLUDE;
		}
		return super.excludeRoute(gRoute);
	}

	private static final String OFF_ONLY = "OFF ONLY";

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		final String tripHeadsign = gTrip.getTripHeadsignOrDefault();
		if (OFF_ONLY.equalsIgnoreCase(tripHeadsign)) {
			return EXCLUDE;
		}
		return super.excludeTrip(gTrip);
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public boolean defaultRouteIdEnabled() {
		return true;
	}

	@Override
	public boolean useRouteShortNameForRouteId() {
		return true;
	}

	@Override
	public boolean defaultRouteLongNameEnabled() {
		return true;
	}

	@NotNull
	@Override
	public String cleanRouteLongName(@NotNull String routeLongName) {
		routeLongName = CleanUtils.toLowerCaseUpperCaseWords(getFirstLanguageNN(), routeLongName);
		return super.cleanRouteLongName(routeLongName);
	}

	@Nullable
	@Override
	public String fixColor(@Nullable String color) {
		if (ColorUtils.BLACK.equals(color)) {
			return null;
		}
		return super.fixColor(color);
	}

	@Nullable
	@Override
	public String provideMissingRouteColor(@NotNull GRoute gRoute) {
		switch (gRoute.getRouteShortName()) {
		case "2S":
			return "13B5EA";
		}
		return super.provideMissingRouteColor(gRoute);
	}

	@Override
	public boolean defaultAgencyColorEnabled() {
		return true;
	}

	private static final String AGENCY_COLOR = "1FB25A";

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public boolean directionSplitterEnabled(long routeId) {
		return true;
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = CleanUtils.toLowerCaseUpperCaseWords(getFirstLanguageNN(), tripHeadsign);
		tripHeadsign = CleanUtils.keepToAndRemoveVia(tripHeadsign);
		tripHeadsign = CleanUtils.cleanBounds(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = CleanUtils.cleanBounds(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	@NotNull
	@Override
	public String getStopCode(@NotNull GStop gStop) {
		//noinspection deprecation
		return gStop.getStopId(); // using stop ID as stop code
	}

	@Override
	public int getStopId(@NotNull GStop gStop) {
		//noinspection deprecation
		final String stopId = gStop.getStopId();
		if (CharUtils.isDigitsOnly(stopId, true)) {
			return Integer.parseInt(stopId);
		}
		throw new MTLog.Fatal("Stop doesn't have an ID (start with) %s!", gStop);
	}
}

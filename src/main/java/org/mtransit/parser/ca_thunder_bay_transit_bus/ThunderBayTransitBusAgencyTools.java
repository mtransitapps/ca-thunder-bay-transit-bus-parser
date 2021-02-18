package org.mtransit.parser.ca_thunder_bay_transit_bus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CharUtils;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// http://www.thunderbay.ca/Living/Getting_Around/Thunder_Bay_Transit/Developers_-_Open_Data.htm
// http://api.nextlift.ca/gtfs.zip
public class ThunderBayTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new ThunderBayTransitBusAgencyTools().start(args);
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

	private static final String OFF_ONLY = "OFF ONLY";

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		if (OFF_ONLY.equalsIgnoreCase(gTrip.getTripHeadsign())) {
			return true; // exclude
		}
		return super.excludeTrip(gTrip);
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	private static final String A = "A";
	private static final String C = "C";
	private static final String J = "J";
	private static final String M = "M";
	private static final String N = "N";
	private static final String S = "S";
	private static final String W = "W";

	private static final long RID_ENDS_WITH_A = 1_000L;
	private static final long RID_ENDS_WITH_C = 3_000L;
	private static final long RID_ENDS_WITH_J = 10_000L;
	private static final long RID_ENDS_WITH_M = 13_000L;
	private static final long RID_ENDS_WITH_N = 14_000L;
	private static final long RID_ENDS_WITH_S = 19_000L;
	private static final long RID_ENDS_WITH_W = 23_000L;

	@Override
	public long getRouteId(@NotNull GRoute gRoute) {
		//noinspection deprecation
		final String routeId = gRoute.getRouteId();
		if (routeId.length() > 0 && CharUtils.isDigitsOnly(routeId)) {
			return Long.parseLong(routeId);
		}
		final Matcher matcher = DIGITS.matcher(routeId);
		if (matcher.find()) {
			final long digits = Long.parseLong(matcher.group());
			if (routeId.endsWith(A)) {
				return digits + RID_ENDS_WITH_A;
			} else if (routeId.endsWith(C)) {
				return digits + RID_ENDS_WITH_C;
			} else if (routeId.endsWith(J)) {
				return digits + RID_ENDS_WITH_J;
			} else if (routeId.endsWith(M)) {
				return digits + RID_ENDS_WITH_M;
			} else if (routeId.endsWith(N)) {
				return digits + RID_ENDS_WITH_N;
			} else if (routeId.endsWith(S)) {
				return digits + RID_ENDS_WITH_S;
			} else if (routeId.endsWith(W)) {
				return digits + RID_ENDS_WITH_W;
			}
		}
		throw new MTLog.Fatal("Can't find route ID for %s!", gRoute);
	}

	@NotNull
	@Override
	public String getRouteLongName(@NotNull GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteLongName())) {
			if (CharUtils.isDigitsOnly(gRoute.getRouteShortName())) {
				final int rsn = Integer.parseInt(gRoute.getRouteShortName());
				switch (rsn) {
				// @formatter:off
				case 1: return "Mainline";
				case 2: return "Crosstown";
				case 4: return "Neebing";
				case 5: return "Edward";
				case 6: return "Mission Rd.";
				case 7: return "Hudson";
				case 8: return "James";
				case 9: return "Junot";
				case 10: return "Northwood";
				case 11: return "John";
				case 12: return "East End";
				case 13: return "John-Jumbo";
				case 14: return "Arthur";
				case 16: return "Balmoral";
				// @formatter:on
				}
			}
			if ("3C".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return "County Park";
			} else if ("3J".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return "Jumbo Gardens";
			} else if ("3M".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return "Memorial";
			}
			throw new MTLog.Fatal("Unexpected route long name '%s'\n!", gRoute);
		}
		return super.getRouteLongName(gRoute);
	}

	@NotNull
	@Override
	public String cleanRouteLongName(@NotNull String routeLongName) {
		routeLongName = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, routeLongName);
		return super.cleanRouteLongName(routeLongName);
	}

	@Nullable
	@Override
	public String getRouteColor(@NotNull GRoute gRoute) {
		if ("2S".equals(gRoute.getRouteShortName()) //
				&& "000000".equals(gRoute.getRouteColor())) {
			return "13B5EA";
		}
		return super.getRouteColor(gRoute);
	}

	private static final String AGENCY_COLOR = "1FB25A";

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public boolean directionSplitterEnabled() {
		return true;
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, tripHeadsign);
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

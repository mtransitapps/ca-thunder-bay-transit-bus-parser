package org.mtransit.parser.ca_thunder_bay_transit_bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.Utils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTripStop;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.mt.data.MTrip;

// http://www.thunderbay.ca/Living/Getting_Around/Thunder_Bay_Transit/Developers_-_Open_Data.htm
// http://api.nextlift.ca/gtfs.zip
public class ThunderBayTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-thunder-bay-transit-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new ThunderBayTransitBusAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		System.out.printf("\nGenerating Thunder Bay Transit bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("\nGenerating Thunder Bay Transit bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
	}

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		return super.excludeRoute(gRoute);
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	private static final String OFF_ONLY = "OFF ONLY";

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (OFF_ONLY.equalsIgnoreCase(gTrip.getTripHeadsign())) {
			return true; // exclude
		}
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

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
	public long getRouteId(GRoute gRoute) {
		String routeId = gRoute.getRouteId();
		if (routeId != null && routeId.length() > 0 && Utils.isDigitsOnly(routeId)) {
			return Long.parseLong(routeId);
		}
		Matcher matcher = DIGITS.matcher(routeId);
		if (matcher.find()) {
			long digits = Long.parseLong(matcher.group());
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
		System.out.printf("\nCan't find route ID for %s!", gRoute);
		System.exit(-1);
		return -1L;
	}

	@Override
	public String getRouteLongName(GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteLongName())) {
			if (Utils.isDigitsOnly(gRoute.getRouteShortName())) {
				int rsn = Integer.parseInt(gRoute.getRouteShortName());
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
			System.out.printf("\nUnexpected route long name '%s'\n!", gRoute);
			System.exit(-1);
			return null;
		}
		return cleanRouteLongName(gRoute);
	}

	private String cleanRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		routeLongName = routeLongName.toLowerCase(Locale.ENGLISH);
		return CleanUtils.cleanLabel(routeLongName);
	}

	@Override
	public String getRouteColor(GRoute gRoute) {
		if ("2S".equals(gRoute.getRouteShortName()) //
				&& "000000".equals(gRoute.getRouteColor())) {
			return "13B5EA";
		}
		return super.getRouteColor(gRoute);
	}

	private static final String AGENCY_COLOR = "1FB25A";

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(4L, new RouteTripSpec(4L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"1615", // 25th Side Rd. & Rosslyn
								"1520", // ++
								"1521", // Arthur & Valhalla Inn
								"1524", // ++
								"1066", // !=
								"1043", // <> Frederica & Brown
								"1067", // !=
								"1827", // ++
								"1019", // City Hall Terminal
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1019", // City Hall Terminal
								"1842", // ==
								"1026", // !=
								"1029", // !=
								"1030", // ==
								"1042", // !=
								"1043", // <> Frederica & Brown
								"1044", // !-
								"1608", // ==
								"1609", // !=
								"1610", // ==
								"1615", // 25th Side Rd. & Rosslyn
						})) //
				.compileBothTripSort());
		map2.put(6L, new RouteTripSpec(6L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "Frederica & Brown", //
				1, MTrip.HEADSIGN_TYPE_STRING, "Anemki") //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"1590", // Anemki & FWFN Office
								"1595", // ++
								"1043", // Frederica & Brown
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"1043", // Frederica & Brown
								"1582", // ++
								"1590", // Anemki & FWFN Office
						})) //
				.compileBothTripSort());
		map2.put(12L, new RouteTripSpec(12L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "Intercity", //
				1, MTrip.HEADSIGN_TYPE_STRING, "City Hall") //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"1019", // City Hall Terminal
								"1548", // ++
								"1006", // Intercity Shopping Centre
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"1006", // Intercity Shopping Centre
								"1850", // ++
								"1019", // City Hall Terminal
						})) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		if (mRoute.getId() == 1L) {
			if (gTrip.getDirectionId() == null) {
				if (gTrip.getTripHeadsign().endsWith(" to City Hall") //
						|| gTrip.getTripHeadsign().endsWith(" to Westfort")) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), 0);
					return;
				}
				if (gTrip.getTripHeadsign().endsWith(" to Waterfront") //
						|| gTrip.getTripHeadsign().endsWith(" to Current River")) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), 1);
					return;
				}
			}
		}
		if (mRoute.getId() == 3L + RID_ENDS_WITH_M) { // 3M
			if (gTrip.getDirectionId() == null) {
				if (gTrip.getTripHeadsign().endsWith(" to City Hall") //
						|| gTrip.getTripHeadsign().endsWith(" to Airport") //
						|| gTrip.getTripHeadsign().endsWith(" to Northwood")) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), 0);
					return;
				}
				if (gTrip.getTripHeadsign().endsWith(" to Waterfront") //
						|| gTrip.getTripHeadsign().endsWith(" to County Park") //
						|| gTrip.getTripHeadsign().endsWith(" to Jumbo Gardens")) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), 1);
					return;
				}
			}
		}
		if (gTrip.getDirectionId() == null) {
			System.out.printf("\n%d, Unepected trips %s!\n", mRoute.getId(), gTrip);
			System.exit(-1);
			return;
		}
		mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), gTrip.getDirectionId());
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		List<String> headsignsValues = Arrays.asList(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignValue());
		if (mTrip.getRouteId() == 1L) {
			if (Arrays.asList( //
					"City Hall", //
					"Westfort" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Westfort", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					"Waterfront", //
					"Current River" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Current River", mTrip.getHeadsignId());
				return true;
			}
		}
		if (mTrip.getRouteId() == 2L) {
			if (Arrays.asList( //
					"University", //
					"City Hall", //
					"Westfort" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Westfort", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					"Machar", //
					"Waterfront" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Waterfront", mTrip.getHeadsignId());
				return true;
			}
		}
		if (mTrip.getRouteId() == 3L + RID_ENDS_WITH_C) { // 3C
			if (Arrays.asList( //
					"City Hall", //
					"Waterfront", //
					"Northwood" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Northwood", mTrip.getHeadsignId());
				return true;
			}
		}
		if (mTrip.getRouteId() == 3L + RID_ENDS_WITH_J) { // 3J
			if (Arrays.asList( //
					"Waterfront", //
					"Airport" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Airport", mTrip.getHeadsignId());
				return true;
			}
		}
		if (mTrip.getRouteId() == 3L + RID_ENDS_WITH_M) { // 3M
			if (Arrays.asList( //
					"Airport", //
					"Northwood", //
					"City Hall" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("City Hall", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					"County Pk", //
					"Jumbo Gdns", //
					"Waterfront" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Waterfront", mTrip.getHeadsignId());
				return true;
			}
		}
		if (mTrip.getRouteId() == 5L) {
			if (Arrays.asList( //
					"College", //
					"Waterfront" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Waterfront", mTrip.getHeadsignId());
				return true;
			}
		}
		if (mTrip.getRouteId() == 8L) {
			if (Arrays.asList( //
					"College", //
					"Intercity" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Intercity", mTrip.getHeadsignId());
				return true;
			}
		}
		if (mTrip.getRouteId() == 9L) {
			if (Arrays.asList( //
					"University", //
					"Intercity" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Intercity", mTrip.getHeadsignId());
				return true;
			}
		}
		if (mTrip.getRouteId() == 10L) {
			if (Arrays.asList( //
					"Waterfront", //
					"City Hall", //
					"County Pk" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("County Pk", mTrip.getHeadsignId());
				return true;
			}
		}
		if (mTrip.getRouteId() == 14L) {
			if (Arrays.asList( //
					"Waterfront", //
					"City Hall", //
					"Jumbo Gdns" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Jumbo Gdns", mTrip.getHeadsignId());
				return true;
			}
		}
		if (mTrip.getRouteId() == 16L) {
			if (Arrays.asList( //
					"Waterfront", //
					"College" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("College", mTrip.getHeadsignId());
				return true;
			}
		}
		System.out.printf("\nUnepected trips to merge %s & %s\n", mTrip, mTripToMerge);
		System.exit(-1);
		return false;
	}

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = CleanUtils.keepToAndRemoveVia(tripHeadsign);
		if (Utils.isUppercaseOnly(tripHeadsign, true, true)) {
			tripHeadsign = tripHeadsign.toLowerCase(Locale.ENGLISH);
		}
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		tripHeadsign = CleanUtils.removePoints(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	@Override
	public String getStopCode(GStop gStop) {
		return gStop.getStopId(); // using stop ID as stop code
	}

	@Override
	public int getStopId(GStop gStop) {
		String stopId = gStop.getStopId();
		if (stopId != null && stopId.length() > 0 && Utils.isDigitsOnly(stopId)) {
			return Integer.valueOf(stopId);
		}
		System.out.printf("\nStop doesn't have an ID (start with) %s!\n", gStop);
		System.exit(-1);
		return -1;
	}
}

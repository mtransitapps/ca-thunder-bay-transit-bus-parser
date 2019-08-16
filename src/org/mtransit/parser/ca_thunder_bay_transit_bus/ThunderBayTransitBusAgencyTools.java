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
				return 1000L + digits;
			} else if (routeId.endsWith(C)) {
				return 3000L + digits;
			} else if (routeId.endsWith(J)) {
				return 10000L + digits;
			} else if (routeId.endsWith(M)) {
				return 13000L + digits;
			} else if (routeId.endsWith(N)) {
				return 14000L + digits;
			} else if (routeId.endsWith(S)) {
				return 19000L + digits;
			} else if (routeId.endsWith(W)) {
				return 23000L + digits;
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

	private static final String SHERWOOD = "Sherwood";
	private static final String WESTFORT = "Westfort";
	private static final String CURRENT_RIVER = "Current River";
	private static final String AIRPORT = "Airport";
	private static final String SHUNIAH = "Shuniah";
	private static final String COLLEGE = "College";
	private static final String CITY_HALL = "City Hall";
	private static final String INTERCITY = "Intercity";
	private static final String WINDSOR = "Windsor";
	private static final String WATERFRONT = "Waterfront";
	private static final String COUNTY_FAIR = "County Fair";
	private static final String CASTLEGREEN = "Castlegreen";

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(1L, new RouteTripSpec(1L, //
				0, MTrip.HEADSIGN_TYPE_STRING, WESTFORT, //
				1, MTrip.HEADSIGN_TYPE_STRING, CURRENT_RIVER) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"1150", // Cowan & Hodder
								"1121", // Waterfront Terminal
								"1000", // Ft. Wm. Rd. & Transit Office (SB)
								"1019", // City Hall Terminal
								"1054", // Mary & Neebing
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"1054", // Mary & Neebing
								"1066", // Brown & Frederica
								"1019", // City Hall Terminal
								"1121", // Waterfront Terminal
								"1150", // Cowan & Hodder
						})) //
				.compileBothTripSort());
		map2.put(2L, new RouteTripSpec(2L, //
				0, MTrip.HEADSIGN_TYPE_STRING, COLLEGE, //
				1, MTrip.HEADSIGN_TYPE_STRING, WATERFRONT) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"1121", // Waterfront Terminal
								"1207", // John & Machar
								"1210", // High & Queen
								"1221", // !=
								"1222", // <> Lakehead University
								"1223", // <>
								"1224", // !=
								"1231", // Confederation College
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"1231", // Confederation College
								"1293", // !=
								"1222", // <> Lakehead University
								"1223", // <>
								"1294", // !=
								"1115", // Court & John
								"1121", // Waterfront Terminal
						})) //
				.compileBothTripSort());
		map2.put(3L + 3000L, new RouteTripSpec(3L + 3000L, // 3C
				0, MTrip.HEADSIGN_TYPE_STRING, CASTLEGREEN, //
				1, MTrip.HEADSIGN_TYPE_STRING, WATERFRONT) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"1121", // Waterfront Terminal
								"1373", // ++
								"1390", // Castlegreen & Superiorview
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"1390", // Castlegreen & Superiorview
								"1424", // ++
								"1121", // Waterfront Terminal
						})) //
				.compileBothTripSort());
		map2.put(3L + 10000L, new RouteTripSpec(3L + 10000L, // 3J
				0, MTrip.HEADSIGN_TYPE_STRING, WATERFRONT, //
				1, MTrip.HEADSIGN_TYPE_STRING, SHERWOOD) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"1418", // Sherwood & Valley
								"1430", // ==
								"1365", // !=
								"1366", // !=
								"1431", // ==
								"1121", // Waterfront Terminal
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"1121", // Waterfront Terminal
								"1371", // ++
								"1418", // Sherwood & Valley
						})) //
				.compileBothTripSort());
		map2.put(3L + 13000L, new RouteTripSpec(3L + 13000L, // 3M
				0, MTrip.HEADSIGN_TYPE_STRING, CITY_HALL, //
				1, MTrip.HEADSIGN_TYPE_STRING, WATERFRONT) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"1121", // Waterfront Terminal
								"1803", // == Memorial & 13th
								"1804", // != Memorial & Dunlop
								"1805", // != Memorial & Isabel
								"1005", // != Ft. Wm. Rd. & Sears
								"1006", // != Intercity Shopping Centre
								"1806", // == May & William
								"1019", // City Hall Terminal
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"1019", // City Hall Terminal
								"1756", // ==
								"1103", // !=
								"1757", // ==
								"1121", // Waterfront Terminal
						})) //
				.compileBothTripSort());
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
		map2.put(5L, new RouteTripSpec(5L, //
				0, MTrip.HEADSIGN_TYPE_STRING, COLLEGE, //
				1, MTrip.HEADSIGN_TYPE_STRING, WESTFORT) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"1269", // Brown & Frederica
								"1231", // Confederation College
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"1231", // Confederation College
								"1269", // Brown & Frederica
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
		map2.put(7L, new RouteTripSpec(7L, //
				0, MTrip.HEADSIGN_TYPE_STRING, SHUNIAH, //
				1, MTrip.HEADSIGN_TYPE_STRING, WATERFRONT) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"1121", // Waterfront Terminal
								"1319", // ==
								"1320", // !=
								"1321", // ==
								"1337", // Shuniah & Erie
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"1337", // Shuniah & Erie
								"1345", // ++
								"1121", // Waterfront Terminal
						})) //
				.compileBothTripSort());
		map2.put(8L, new RouteTripSpec(8L, //
				0, MTrip.HEADSIGN_TYPE_STRING, INTERCITY, // COLLEGE_S_
				1, MTrip.HEADSIGN_TYPE_STRING, CITY_HALL) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"1019", // City Hall Terminal
								"1231", // Confederation College
								"1006", // Intercity Shopping Centre
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"1006", // Intercity Shopping Centre
								"1231", // Confederation College
								"1019", // City Hall Terminal
						})) //
				.compileBothTripSort());
		map2.put(9L, new RouteTripSpec(9L, //
				0, MTrip.HEADSIGN_TYPE_STRING, INTERCITY, //
				1, MTrip.HEADSIGN_TYPE_STRING, WATERFRONT) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"1121", // Waterfront Terminal
								"1467", // == River & Balsam
								"1853", // ??? Junot & John ???
								"1468", // == River & High
								//
								"1476", // == Junot & Windsor
								"1853", // Junot & John
								"1477", // == Golf Links & John
								"1222", // Lakehead University
								"1006", // Intercity Shopping Centre
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"1006", // Intercity Shopping Centre
								"1222", // Lakehead University
								"1121", // Waterfront Terminal
						})) //
				.compileBothTripSort());
		map2.put(10L, new RouteTripSpec(10L, //
				0, MTrip.HEADSIGN_TYPE_STRING, COLLEGE, //
				1, MTrip.HEADSIGN_TYPE_STRING, CITY_HALL) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"1019", // City Hall Terminal
								"1255", // ==
								"1848", // <>
								"1849", // <>
								"1256", // ==
								"1231", // Confederation College
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"1231", // Confederation College
								"1244", // ==
								"1848", // <>
								"1849", // <>
								"1245", // ==
								"1019", // City Hall Terminal
						})) //
				.compileBothTripSort());
		map2.put(11L, new RouteTripSpec(11L, //
				0, MTrip.HEADSIGN_TYPE_STRING, WINDSOR, //
				1, MTrip.HEADSIGN_TYPE_STRING, WATERFRONT) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"1121", // Waterfront Terminal
								"1450", // Windsor & Junot
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"1450", // Windsor & Junot
								"1121", // Waterfront Terminal
						})) //
				.compileBothTripSort());
		map2.put(12L, new RouteTripSpec(12L, //
				0, MTrip.HEADSIGN_TYPE_STRING, INTERCITY, //
				1, MTrip.HEADSIGN_TYPE_STRING, CITY_HALL) //
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
		map2.put(13L, new RouteTripSpec(13L, //
				0, MTrip.HEADSIGN_TYPE_STRING, COUNTY_FAIR, //
				1, MTrip.HEADSIGN_TYPE_STRING, WATERFRONT) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"1121", // Waterfront Terminal
								"1445", // ++
								"1423", // Dawson & Regina
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"1423", // Dawson & Regina
								"1306", // Algoma & John
								"1121", // Waterfront Terminal
						})) //
				.compileBothTripSort());
		map2.put(14L, new RouteTripSpec(14L, //
				0, MTrip.HEADSIGN_TYPE_STRING, AIRPORT, //
				1, MTrip.HEADSIGN_TYPE_STRING, CITY_HALL) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"1019", // City Hall Terminal
								"1513", // ==
								"1514", // !=
								"1521", // !=
								"1522", // == Thunder Bay Airport
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"1522", // Thunder Bay Airport
								"1531", // ++
								"1019", // City Hall Terminal
						})) //
				.compileBothTripSort());
		map2.put(16L, new RouteTripSpec(16L, //
				0, MTrip.HEADSIGN_TYPE_STRING, COLLEGE, //
				1, MTrip.HEADSIGN_TYPE_STRING, CITY_HALL) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"1019", // City Hall Terminal
								"1839", // ++
								"1231", // Confederation College
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"1231", // Confederation College
								"1573", // ++
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
		mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), gTrip.getDirectionId());
	}

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
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

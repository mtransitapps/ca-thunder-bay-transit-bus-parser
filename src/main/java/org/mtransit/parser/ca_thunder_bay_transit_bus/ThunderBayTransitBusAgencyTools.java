package org.mtransit.parser.ca_thunder_bay_transit_bus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.StringUtils;
import org.mtransit.parser.Utils;
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
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// http://www.thunderbay.ca/Living/Getting_Around/Thunder_Bay_Transit/Developers_-_Open_Data.htm
// http://api.nextlift.ca/gtfs.zip
public class ThunderBayTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(@Nullable String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-thunder-bay-transit-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new ThunderBayTransitBusAgencyTools().start(args);
	}

	@Nullable
	private HashSet<Integer> serviceIdInts;

	@Override
	public void start(@NotNull String[] args) {
		MTLog.log("Generating Thunder Bay Transit bus data...");
		long start = System.currentTimeMillis();
		this.serviceIdInts = extractUsefulServiceIdInts(args, this, true);
		super.start(args);
		MTLog.log("Generating Thunder Bay Transit bus data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIdInts != null && this.serviceIdInts.isEmpty();
	}

	@Override
	public boolean excludeCalendar(@NotNull GCalendar gCalendar) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarInt(gCalendar, this.serviceIdInts);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(@NotNull GCalendarDate gCalendarDates) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarDateInt(gCalendarDates, this.serviceIdInts);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	private static final String OFF_ONLY = "OFF ONLY";

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		if (OFF_ONLY.equalsIgnoreCase(gTrip.getTripHeadsign())) {
			return true; // exclude
		}
		if (this.serviceIdInts != null) {
			return excludeUselessTripInt(gTrip, this.serviceIdInts);
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
		if (routeId.length() > 0 && Utils.isDigitsOnly(routeId)) {
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
		throw new MTLog.Fatal("Can't find route ID for %s!", gRoute);
	}

	@NotNull
	@Override
	public String getRouteLongName(@NotNull GRoute gRoute) {
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
			throw new MTLog.Fatal("Unexpected route long name '%s'\n!", gRoute);
		}
		return cleanRouteLongName(gRoute);
	}

	private String cleanRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		routeLongName = routeLongName.toLowerCase(Locale.ENGLISH);
		return CleanUtils.cleanLabel(routeLongName);
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

	private static final HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;

	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<>();
		//noinspection deprecation
		map2.put(4L, new RouteTripSpec(4L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(//
								"1615", // 25th Side Rd. & Rosslyn
								"1520", // ++
								"1521", // Arthur & Valhalla Inn
								"1524", // ++
								"1066", // !=
								"1043", // <> Frederica & Brown
								"1067", // !=
								"1827", // ++
								"1019" // City Hall Terminal
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(//
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
								"1615" // 25th Side Rd. & Rosslyn
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(6L, new RouteTripSpec(6L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "Frederica & Brown", //
				1, MTrip.HEADSIGN_TYPE_STRING, "Anemki") //
				.addTripSort(0, //
						Arrays.asList(//
								"1590", // Anemki & FWFN Office
								"1595", // ++
								"1043" // Frederica & Brown
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"1043", // Frederica & Brown
								"1582", // ++
								"1590" // Anemki & FWFN Office
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(8L, new RouteTripSpec(8L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "Intercity", //
				1, MTrip.HEADSIGN_TYPE_STRING, "City Hall") //
				.addTripSort(0, //
						Arrays.asList(//
								"1019", // City Hall Terminal
								"1231", // Confederation College
								"1006" // Intercity Shopping Centre
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"1006", // Intercity Shopping Centre
								"1231", // Confederation College
								"1019" // City Hall Terminal

						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(12L, new RouteTripSpec(12L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "Intercity", //
				1, MTrip.HEADSIGN_TYPE_STRING, "City Hall") //
				.addTripSort(0, //
						Arrays.asList(//
								"1019", // City Hall Terminal
								"1548", // ++
								"1006" // Intercity Shopping Centre
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"1006", // Intercity Shopping Centre
								"1850", // ++
								"1019" // City Hall Terminal
						)) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, @NotNull List<MTripStop> list1, @NotNull List<MTripStop> list2, @NotNull MTripStop ts1, @NotNull MTripStop ts2, @NotNull GStop ts1GStop, @NotNull GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@NotNull
	@Override
	public ArrayList<MTrip> splitTrip(@NotNull MRoute mRoute, @Nullable GTrip gTrip, @NotNull GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@NotNull
	@Override
	public Pair<Long[], Integer[]> splitTripStop(@NotNull MRoute mRoute, @NotNull GTrip gTrip, @NotNull GTripStop gTripStop, @NotNull ArrayList<MTrip> splitTrips, @NotNull GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(@NotNull MRoute mRoute, @NotNull MTrip mTrip, @NotNull GTrip gTrip, @NotNull GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		if (gTrip.getDirectionId() == null) {
			if (mRoute.getId() == 1L) {
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
			if (mRoute.getId() == 2L) {
				if (gTrip.getTripHeadsign().endsWith(" to City Hall") //
						|| gTrip.getTripHeadsign().endsWith(" to University") //
						|| gTrip.getTripHeadsign().endsWith(" to Westfort")) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), 0);
					return;
				}
				if (gTrip.getTripHeadsign().endsWith(" to Machar") //
						|| gTrip.getTripHeadsign().endsWith(" to Waterfront")) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), 1);
					return;
				}
			}
			if (mRoute.getId() == 3L + RID_ENDS_WITH_C) { // 3C
				if (gTrip.getTripHeadsign().endsWith(" to Castlegreen Dr")) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), 0);
					return;
				}
				if (gTrip.getTripHeadsign().endsWith(" to Northwood") //
						|| gTrip.getTripHeadsign().endsWith(" to City Hall") //
						|| gTrip.getTripHeadsign().endsWith(" to Waterfront")) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), 1);
					return;
				}
			}
			if (mRoute.getId() == 3L + RID_ENDS_WITH_J) { // 3J
				if (gTrip.getTripHeadsign().endsWith(" to Sherwood Dr.")) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), 0);
					return;
				}
				if (gTrip.getTripHeadsign().endsWith(" to Airport") //
						|| gTrip.getTripHeadsign().endsWith(" to Waterfront")) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), 1);
					return;
				}
			}
			if (mRoute.getId() == 3L + RID_ENDS_WITH_M) { // 3M
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
			if (mRoute.getId() == 5L) {
				if (gTrip.getTripHeadsign().endsWith(" to Westfort")) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), 0);
					return;
				}
				if (gTrip.getTripHeadsign().endsWith(" to Waterfront") //
						|| gTrip.getTripHeadsign().endsWith(" to College")) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), 1);
					return;
				}
			}
			if (mRoute.getId() == 7L) {
				if (gTrip.getTripHeadsign().endsWith(" to Shuniah St.")) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), 0);
					return;
				}
				if (gTrip.getTripHeadsign().endsWith(" to Waterfront")) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), 1);
					return;
				}
			}
			if (mRoute.getId() == 8L) {
				if (gTrip.getTripHeadsign().endsWith(" to Intercity") //
						|| gTrip.getTripHeadsign().endsWith(" to College")) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), 0);
					return;
				}
				if (gTrip.getTripHeadsign().endsWith(" to City Hall")) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), 1);
					return;
				}
			}
			if (mRoute.getId() == 9L) {
				if (gTrip.getTripHeadsign().endsWith(" to Intercity") //
						|| gTrip.getTripHeadsign().endsWith(" to University")) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), 0);
					return;
				}
				if (gTrip.getTripHeadsign().endsWith(" to Waterfront")) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), 1);
					return;
				}
			}
			if (mRoute.getId() == 10L) {
				if (gTrip.getTripHeadsign().endsWith(" to College")) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), 0);
					return;
				}
				if (gTrip.getTripHeadsign().endsWith(" to County Park") //
						|| gTrip.getTripHeadsign().endsWith(" to City Hall") //
						|| gTrip.getTripHeadsign().endsWith(" to Waterfront")) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), 1);
					return;
				}
			}
			if (mRoute.getId() == 11L) {
				if (gTrip.getTripHeadsign().endsWith(" to Windsor St.")) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), 0);
					return;
				}
				if (gTrip.getTripHeadsign().endsWith(" to Waterfront")) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), 1);
					return;
				}
			}
			if (mRoute.getId() == 13L) {
				if (gTrip.getTripHeadsign().endsWith(" to County Fair")) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), 0);
					return;
				}
				if (gTrip.getTripHeadsign().endsWith(" to Waterfront")) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), 1);
					return;
				}
			}
			if (mRoute.getId() == 14L) {
				if (gTrip.getTripHeadsign().endsWith(" to Airport")) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), 0);
					return;
				}
				if (gTrip.getTripHeadsign().endsWith(" to Jumbo Gardens") //
						|| gTrip.getTripHeadsign().endsWith(" to Waterfront") //
						|| gTrip.getTripHeadsign().endsWith(" to City Hall")) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), 1);
					return;
				}
			}
			if (mRoute.getId() == 16L) {
				if (gTrip.getTripHeadsign().endsWith(" to College") //
						|| gTrip.getTripHeadsign().endsWith(" to Waterfront")) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), 0);
					return;
				}
				if (gTrip.getTripHeadsign().endsWith(" to City Hall")) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), 1);
					return;
				}
			}
			throw new MTLog.Fatal("%d, Unexpected trips %s!", mRoute.getId(), gTrip);
		}
		mTrip.setHeadsignString(
				cleanTripHeadsign(gTrip.getTripHeadsignOrDefault()),
				gTrip.getDirectionIdOrDefault()
		);
	}

	@Override
	public boolean directionFinderEnabled() {
		return false; // DISABLED because direction_id NOT provided
	}

	@Override
	public boolean mergeHeadsign(@NotNull MTrip mTrip, @NotNull MTrip mTripToMerge) {
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
		throw new MTLog.Fatal("Unexpected trips to merge %s & %s", mTrip, mTripToMerge);
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
		if (stopId != null && stopId.length() > 0 && Utils.isDigitsOnly(stopId)) {
			return Integer.parseInt(stopId);
		}
		throw new MTLog.Fatal("Stop doesn't have an ID (start with) %s!", gStop);
	}
}

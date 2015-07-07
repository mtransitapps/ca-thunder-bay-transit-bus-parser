package org.mtransit.parser.ca_thunder_bay_transit_bus;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
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
		System.out.printf("\nGenerating Thunder Bay Transit bus data...\n");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("\nGenerating Thunder Bay Transit bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
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

	@Override
	public boolean excludeTrip(GTrip gTrip) {
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
		String routeId = gRoute.route_id;
		if (routeId != null && routeId.length() > 0 && Utils.isDigitsOnly(routeId)) {
			return Integer.valueOf(routeId); // using stop code as stop ID
		}
		Matcher matcher = DIGITS.matcher(routeId);
		matcher.find();
		int digits = Integer.parseInt(matcher.group());
		if (routeId.endsWith(A)) {
			return 1000 + digits;
		} else if (routeId.endsWith(C)) {
			return 3000 + digits;
		} else if (routeId.endsWith(J)) {
			return 10000 + digits;
		} else if (routeId.endsWith(M)) {
			return 13000 + digits;
		} else if (routeId.endsWith(N)) {
			return 14000 + digits;
		} else if (routeId.endsWith(S)) {
			return 19000 + digits;
		} else if (routeId.endsWith(W)) {
			return 23000 + digits;
		}
		System.out.println("Can't find route ID for " + gRoute);
		System.exit(-1);
		return -1l;
	}

	private static final String COLOR_000000 = "000000";
	private static final String COLOR_13B5EA = "13B5EA";

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (ROUTE_SN_2S.equals(gRoute.route_short_name) && COLOR_000000.equals(gRoute.route_color)) {
			return COLOR_13B5EA;
		}
		return super.getRouteColor(gRoute);
	}

	private static final String AGENCY_COLOR = "1FB25A";

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String ROUTE_SN_1 = "1";
	private static final String ROUTE_SN_2 = "2";
	private static final String ROUTE_SN_2W = "2W";
	private static final String ROUTE_SN_2S = "2S";
	private static final String ROUTE_SN_3A = "3A";
	private static final String ROUTE_SN_3C = "3C";
	private static final String ROUTE_SN_3J = "3J";
	private static final String ROUTE_SN_3M = "3M";
	private static final String ROUTE_SN_3N = "3N";
	private static final String ROUTE_SN_4 = "4";
	private static final String ROUTE_SN_6 = "6";
	private static final String ROUTE_SN_7 = "7";
	private static final String ROUTE_SN_8 = "8";
	private static final String ROUTE_SN_9 = "9";
	private static final String ROUTE_SN_11 = "11";
	private static final String ROUTE_SN_12 = "12";
	private static final String ROUTE_SN_13 = "13";
	private static final String ROUTE_SN_20 = "20";

	private static final String SHERWOOD = "Sherwood";
	private static final String WESTFORT = "Westfort";
	private static final String CURRENT_RIVER = "Current River";
	private static final String AIRPORT = "Airport";
	private static final String JUMBO_GARDENS = "Jumbo Gardens";
	private static final String COUNTY_PARK = "County Park";
	private static final String NORTHWOOD = "Northwood";
	private static final String NEEBING = "Neebing";
	private static final String MISSION = "Mission";
	private static final String SHUNIAH = "Shuniah";
	private static final String COLLEGE = "College";
	private static final String CITY_HALL = "City Hall";
	private static final String UNIVERSITY = "University";
	private static final String INTERCITY = "Intercity";
	private static final String COLLEGE_S_INTERCITY = COLLEGE + " / " + INTERCITY;
	private static final String UNIVERSITY_S_INTERCITY = UNIVERSITY + " / " + INTERCITY;
	private static final String WINDSOR = "Windsor";
	private static final String EAST_END = "East End";
	private static final String WATERFRONT = "Waterfront";
	private static final String WATERFRONT_COLLEGE = WATERFRONT + " (" + COLLEGE + ")";
	private static final String COUNTY_FAIR = "County Fair";
	private static final String EXPRESS = "Express";
	private static final String CASTLEGREEN = "Castlegreen";

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		String stationName = null;
		int directionId = -1;
		String routeShortName = mRoute.shortName;
		if (ROUTE_SN_1.equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith(WESTFORT) || gTrip.trip_headsign.endsWith(CITY_HALL)) {
				directionId = 0;
				stationName = WESTFORT; // South
			} else if (gTrip.trip_headsign.endsWith(CURRENT_RIVER) || gTrip.trip_headsign.endsWith(WATERFRONT)) {
				directionId = 1;
				stationName = CURRENT_RIVER; // North
			}
		} else if (ROUTE_SN_2.equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith(CITY_HALL)) {
				directionId = 0;
				stationName = CITY_HALL; // South
			} else if (gTrip.trip_headsign.endsWith(WATERFRONT)) {
				directionId = 1;
				stationName = WATERFRONT; // North
			}
		} else if (ROUTE_SN_2W.equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith(COLLEGE) || gTrip.trip_headsign.endsWith(WATERFRONT)) {
				directionId = 0;
				stationName = WATERFRONT_COLLEGE; // North
			} else if (gTrip.trip_headsign.endsWith(WESTFORT)) {
				directionId = 1;
				stationName = WESTFORT; // South
			}
		} else if (ROUTE_SN_2S.equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith(COLLEGE)) {
				directionId = 0;
				stationName = COLLEGE; // North
			} else if (gTrip.trip_headsign.endsWith(WESTFORT)) {
				directionId = 1;
				stationName = WESTFORT; // South
			}
		} else if (ROUTE_SN_3A.equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith(AIRPORT)) {
				directionId = 0;
				stationName = AIRPORT; // West
			} else if (gTrip.trip_headsign.endsWith(CITY_HALL) || gTrip.trip_headsign.endsWith(JUMBO_GARDENS) || gTrip.trip_headsign.endsWith(WATERFRONT)) {
				directionId = 1;
				stationName = CITY_HALL; // East
			}
		} else if (ROUTE_SN_3C.equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith(COUNTY_PARK)) {
				directionId = 0;
				stationName = CASTLEGREEN; // North / West
			} else if (gTrip.trip_headsign.endsWith(WATERFRONT) || gTrip.trip_headsign.endsWith(CITY_HALL) || gTrip.trip_headsign.endsWith(NORTHWOOD)) {
				directionId = 1;
				stationName = WATERFRONT; // South / East
			}
		} else if (ROUTE_SN_3J.equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith(WATERFRONT) || gTrip.trip_headsign.endsWith(AIRPORT)) {
				directionId = 0;
				stationName = WATERFRONT; // South / East
			} else if (gTrip.trip_headsign.endsWith(JUMBO_GARDENS)) {
				directionId = 1;
				stationName = SHERWOOD; // North / West
			}
		} else if (ROUTE_SN_3M.equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith(CITY_HALL) || gTrip.trip_headsign.endsWith(AIRPORT) || gTrip.trip_headsign.endsWith(NORTHWOOD)) {
				directionId = 0;
				stationName = CITY_HALL; // South
			} else if (gTrip.trip_headsign.endsWith(WATERFRONT) || gTrip.trip_headsign.endsWith(COUNTY_PARK) || gTrip.trip_headsign.endsWith(JUMBO_GARDENS)) {
				directionId = 1;
				stationName = WATERFRONT; // North
			}
		} else if (ROUTE_SN_3N.equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith(COUNTY_PARK) || gTrip.trip_headsign.endsWith(WATERFRONT) || gTrip.trip_headsign.endsWith(CITY_HALL)) {
				directionId = 0;
				stationName = CITY_HALL; // South / East
			} else if (gTrip.trip_headsign.endsWith(NORTHWOOD)) {
				directionId = 1;
				stationName = COLLEGE; // North / West
			}
		} else if (ROUTE_SN_4.equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith(NEEBING)) {
				directionId = 0;
				stationName = NEEBING; // Circle
			}
		} else if (ROUTE_SN_6.equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith(MISSION)) {
				directionId = 0;
				stationName = MISSION; // Circle
			}
		} else if (ROUTE_SN_7.equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith(SHUNIAH)) {
				directionId = 0;
				stationName = SHUNIAH; // North
			} else if (gTrip.trip_headsign.endsWith(WATERFRONT)) {
				directionId = 1;
				stationName = WATERFRONT; // South
			}
		} else if (ROUTE_SN_8.equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith(INTERCITY) || gTrip.trip_headsign.endsWith(COLLEGE)) {
				directionId = 0;
				stationName = COLLEGE_S_INTERCITY; // North
			} else if (gTrip.trip_headsign.endsWith(CITY_HALL)) {
				directionId = 1;
				stationName = CITY_HALL; // South
			}
		} else if (ROUTE_SN_9.equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith(INTERCITY) || gTrip.trip_headsign.endsWith(UNIVERSITY)) {
				directionId = 0;
				stationName = UNIVERSITY_S_INTERCITY; // South
			} else if (gTrip.trip_headsign.endsWith(WATERFRONT)) {
				directionId = 1;
				stationName = WATERFRONT; // North
			}
		} else if (ROUTE_SN_11.equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith(WINDSOR)) {
				directionId = 0;
				stationName = WINDSOR; // West
			} else if (gTrip.trip_headsign.endsWith(WATERFRONT)) {
				directionId = 1;
				stationName = WATERFRONT; // East
			}
		} else if (ROUTE_SN_12.equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith(EAST_END)) {
				directionId = 0;
				stationName = EAST_END; // Circle
			}
		} else if (ROUTE_SN_13.equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith(COUNTY_FAIR)) {
				directionId = 0;
				stationName = COUNTY_FAIR; // North / West
			} else if (gTrip.trip_headsign.endsWith(WATERFRONT)) {
				directionId = 1;
				stationName = WATERFRONT; // South / East
			}
		} else if (ROUTE_SN_20.equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith(EXPRESS)) {
				directionId = 0;
				stationName = EXPRESS; // Circle
			}
		}
		if (stationName == null || directionId < 0) {
			System.out.println("Unexpected trip " + gTrip);
			System.exit(-1);
		}
		mTrip.setHeadsignString(stationName, directionId);
	}

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
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
		return gStop.stop_id; // using stop ID as stop code
	}

	@Override
	public int getStopId(GStop gStop) {
		String stopId = gStop.stop_id;
		if (stopId != null && stopId.length() > 0 && Utils.isDigitsOnly(stopId)) {
			return Integer.valueOf(stopId); // using stop code as stop ID
		}
		System.out.println("Stop doesn't have an ID (start with)! " + gStop);
		System.exit(-1);
		return -1;
	}
}

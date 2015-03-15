package org.mtransit.parser.ca_thunder_bay_transit_bus;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MSpec;
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
		System.out.printf("Generating Thunder Bay Transit bus data...\n");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("Generating Thunder Bay Transit bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
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

	@Override
	public long getRouteId(GRoute gRoute) {
		String routeId = gRoute.route_id;
		if (routeId != null && routeId.length() > 0 && Utils.isDigitsOnly(routeId)) {
			return Integer.valueOf(routeId); // using stop code as stop ID
		}
		Matcher matcher = DIGITS.matcher(routeId);
		matcher.find();
		int digits = Integer.parseInt(matcher.group());
		if (routeId.endsWith("A")) {
			return 1000 + digits;
		} else if (routeId.endsWith("C")) {
			return 3000 + digits;
		} else if (routeId.endsWith("J")) {
			return 10000 + digits;
		} else if (routeId.endsWith("M")) {
			return 13000 + digits;
		} else if (routeId.endsWith("N")) {
			return 14000 + digits;
		} else if (routeId.endsWith("W")) {
			return 23000 + digits;
		} else {
			System.out.println("Can't find route ID for " + gRoute);
			System.exit(-1);
			return -1;
		}
	}

	private static final String AGENCY_COLOR = "1FB25A";

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public void setTripHeadsign(MRoute route, MTrip mTrip, GTrip gTrip) {
		String stationName = null;
		int directionId = -1;
		String routeShortName = route.shortName;
		if ("1".equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith("Westfort") || gTrip.trip_headsign.endsWith("City Hall")) {
				directionId = 0;
				stationName = "Westfort"; // South
			} else if (gTrip.trip_headsign.endsWith("Current River") || gTrip.trip_headsign.endsWith("Waterfront")) {
				directionId = 1;
				stationName = "Current River"; // North
			}
		} else if ("2".equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith("City Hall")) {
				directionId = 0;
				stationName = "City Hall"; // South
			} else if (gTrip.trip_headsign.endsWith("Waterfront")) {
				directionId = 1;
				stationName = "Waterfront"; // North
			}
		} else if ("2W".equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith("College") || gTrip.trip_headsign.endsWith("Waterfront")) {
				directionId = 0;
				stationName = "Waterfront (College)"; // North
			} else if (gTrip.trip_headsign.endsWith("Westfort")) {
				directionId = 1;
				stationName = "Westfort"; // South
			}
		} else if ("3A".equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith("Airport")) {
				directionId = 0;
				stationName = "Airport"; // West
			} else if (gTrip.trip_headsign.endsWith("City Hall") || gTrip.trip_headsign.endsWith("Jumbo Gardens") || gTrip.trip_headsign.endsWith("Waterfront")) {
				directionId = 1;
				stationName = "City Hall"; // East
			}
		} else if ("3C".equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith("County Park")) {
				directionId = 0;
				stationName = "Castlegreen"; // North / West
			} else if (gTrip.trip_headsign.endsWith("Waterfront") || gTrip.trip_headsign.endsWith("City Hall") || gTrip.trip_headsign.endsWith("Northwood")) {
				directionId = 1;
				stationName = "Waterfront"; // South / East
			}
		} else if ("3J".equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith("Waterfront") || gTrip.trip_headsign.endsWith("Airport")) {
				directionId = 0;
				stationName = "Waterfront"; // South / East
			} else if (gTrip.trip_headsign.endsWith("Jumbo Gardens")) {
				directionId = 1;
				stationName = "Sherwood"; // North / West
			}
		} else if ("3M".equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith("City Hall") || gTrip.trip_headsign.endsWith("Airport") || gTrip.trip_headsign.endsWith("Northwood")) {
				directionId = 0;
				stationName = "City Hall"; // South
			} else if (gTrip.trip_headsign.endsWith("Waterfront") || gTrip.trip_headsign.endsWith("County Park")
					|| gTrip.trip_headsign.endsWith("Jumbo Gardens")) {
				directionId = 1;
				stationName = "Waterfront"; // North
			}
		} else if ("3N".equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith("County Park") || gTrip.trip_headsign.endsWith("Waterfront")) {
				directionId = 0;
				stationName = "City Hall"; // South / East
			} else if (gTrip.trip_headsign.endsWith("Northwood")) {
				directionId = 1;
				stationName = "College"; // North / West
			}
		} else if ("4".equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith("Neebing")) {
				directionId = 0;
				stationName = "Neebing"; // Circle
			}
		} else if ("6".equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith("Mission")) {
				directionId = 0;
				stationName = "Mission"; // Circle
			}
		} else if ("7".equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith("Shuniah")) {
				directionId = 0;
				stationName = "Shuniah"; // North
			} else if (gTrip.trip_headsign.endsWith("Waterfront")) {
				directionId = 1;
				stationName = "Waterfront"; // South
			}
		} else if ("8".equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith("Intercity") || gTrip.trip_headsign.endsWith("College")) {
				directionId = 0;
				stationName = "College / Intercity"; // North
			} else if (gTrip.trip_headsign.endsWith("City Hall")) {
				directionId = 1;
				stationName = "City Hall"; // South
			}
		} else if ("9".equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith("Intercity") || gTrip.trip_headsign.endsWith("University")) {
				directionId = 0;
				stationName = "University / Intercity"; // South
			} else if (gTrip.trip_headsign.endsWith("Waterfront")) {
				directionId = 1;
				stationName = "Waterfront"; // North
			}
		} else if ("11".equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith("Windsor")) {
				directionId = 0;
				stationName = "Windsor"; // West
			} else if (gTrip.trip_headsign.endsWith("Waterfront")) {
				directionId = 1;
				stationName = "Waterfront"; // East
			}
		} else if ("12".equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith("East End")) {
				directionId = 0;
				stationName = "East End"; // Circle
			}
		} else if ("13".equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith("County Fair")) {
				directionId = 0;
				stationName = "County Fair"; // North / West
			} else if (gTrip.trip_headsign.endsWith("Waterfront")) {
				directionId = 1;
				stationName = "Waterfront"; // South / East
			}
		} else if ("20".equals(routeShortName)) {
			if (gTrip.trip_headsign.endsWith("Express")) {
				directionId = 0;
				stationName = "Express"; // Circle
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
		return MSpec.cleanLabel(tripHeadsign);
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

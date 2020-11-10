package com.github.warren_bank.myplaces.parsers;

import com.github.warren_bank.myplaces.models.WaypointListItem;

    // =====================================================
    // JOOX api:
    //     https://www.jooq.org/products/jOOX/javadoc/current/allclasses-noframe.html
    //
    //     https://www.jooq.org/products/jOOX/javadoc/current/org/joox/JOOX.html
    //     https://www.jooq.org/products/jOOX/javadoc/current/org/joox/Match.html
    //     https://www.jooq.org/products/jOOX/javadoc/current/org/joox/Each.html
    //     https://www.jooq.org/products/jOOX/javadoc/current/org/joox/Context.html
    //     https://www.jooq.org/products/jOOX/javadoc/current/org/joox/Filter.html
    //     https://www.jooq.org/products/jOOX/javadoc/current/org/joox/FastFilter.html
    //
    //     https://docs.oracle.com/javase/8/docs/api/org/w3c/dom/Element.html
    // =====================================================
import org.joox.JOOX;
import org.joox.Match;
import org.joox.Each;
import org.joox.Context;
import org.joox.Filter;
import org.joox.FastFilter;

import org.w3c.dom.Element;

import java.util.ArrayList;

public class KmlParser extends AbstractParser {
    protected int nonce;

    private static final FastFilter is_name  = JOOX.tag("name",         true);
    private static final FastFilter is_desc  = JOOX.tag("description",  true);

    private static final Filter is_name_tag  = new Filter() {
        @Override
        public boolean filter(Context context) {
            return (
                is_name.filter(context) ||
                is_desc.filter(context)
            );
        }
    };

    public KmlParser(String filepath) {
        super(filepath);
    }

    @Override
    public ArrayList<WaypointListItem> parse() {
        ArrayList<WaypointListItem> arrayList = new ArrayList<WaypointListItem>();
        this.nonce = 0;

        // =================================================
        // KML spec:
        //     http://dagik.org/kml_intro/E/point.html
        // =================================================

        Match points;
        try {
            points = getXmlRoot().find("Placemark");
        }
        catch(Exception e) {
            return arrayList;
        }

        // =================================================
        // required child nodes: <Point><coordinates>lon,lat,altitude</coordinates></Point>
        // optional child nodes: <name>, <description>
        // =================================================

        points.each(new Each(){
            @Override
            public void each(Context context) {
                Match el = JOOX.$(context.element());

                String name = el.find(is_name_tag).text();         // first value, or null if not found
                if (name != null) {
                    name = name.trim();
                }

                String strCoords = el.find("coordinates").text();  // CSV, or null if not found
                if (strCoords == null) return;

                String[] arrCoords = strCoords.trim().split("\\s*,\\s*");
                if (arrCoords.length < 2) return;

                String lon = arrCoords[0];
                String lat = arrCoords[1];

                arrayList.add(
                    new WaypointListItem(lat, lon, name, KmlParser.this.nonce++)
                );
            }
        });

        return arrayList;
    }
}

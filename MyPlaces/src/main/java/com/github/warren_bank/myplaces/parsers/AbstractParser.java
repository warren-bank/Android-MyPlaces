package com.github.warren_bank.myplaces.parsers;

import com.github.warren_bank.myplaces.models.WaypointListItem;

    // =====================================================
    // JOOX api:
    //     https://www.jooq.org/products/jOOX/javadoc/current/allclasses-noframe.html
    //
    //     https://www.jooq.org/products/jOOX/javadoc/current/org/joox/JOOX.html
    //     https://www.jooq.org/products/jOOX/javadoc/current/org/joox/Match.html
    // =====================================================
import org.joox.JOOX;
import org.joox.Match;

import java.util.ArrayList;
import java.io.File;
import java.io.IOException;

import org.xml.sax.SAXException;

public abstract class AbstractParser {
    private File  xmlFile;
    private Match xmlRoot;

    public AbstractParser(String filepath) {
        xmlFile = new File(filepath);

        try {
            getXmlRoot();
        }
        catch (Exception e){}
    }

    public abstract ArrayList<WaypointListItem> parse();

    protected Match getXmlRoot() throws SAXException, IOException {
        if (xmlRoot == null) {
            xmlRoot = JOOX.$(xmlFile);
        }
        return xmlRoot;
    }
}

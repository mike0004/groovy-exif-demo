#!/usr/bin/env groovy 

/**
*
* Run this via `groovy GroovyEdifDemo.groovy sample-exif.jpg`
*   or
* Run it as a script `GroovyEdifDemo.groovy sample-exif.jpg`
* 
* Refs:
*
* taken from: http://hohonuuli.blogspot.com/2010/10/extracting-image-metadata-using.html
* @See: http://groovy.codehaus.org/Grape -- depedencies
*/


@Grab(group='org.apache.sanselan', module='sanselan', version='0.97-incubator')
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import javax.imageio.metadata.IIOMetadata
import javax.imageio.stream.ImageInputStream
import org.apache.sanselan.ImageReadException
import org.apache.sanselan.Sanselan
import org.apache.sanselan.common.IImageMetadata
import org.apache.sanselan.common.RationalNumber
import org.apache.sanselan.formats.jpeg.JpegImageMetadata
import org.apache.sanselan.formats.tiff.TiffField
import org.apache.sanselan.formats.tiff.TiffImageMetadata
import org.apache.sanselan.formats.tiff.constants.GPSTagConstants
import org.apache.sanselan.formats.tiff.constants.TagInfo
import org.apache.sanselan.formats.tiff.constants.TiffConstants
import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node

def file = new File(args[0])
println("file: ${file.getPath()}")
println("---- Extracting metadata using Sanselan")
printSanselan(file)
println("---- Extracting XMP XML using Sanselan")
printSanselanXMP(file)
println("---- Extracting metadata using Java ImageIO")
printImageIO(file)



void printImageIO(File file) {
    
    ImageInputStream input = ImageIO.createImageInputStream(file)
    Iterator readers = ImageIO.getImageReaders(input)
    while (readers.hasNext()) {
        ImageReader reader = readers.next()
        reader.setInput(input, true)
        IIOMetadata metadata = reader.getImageMetadata(0)
        String[] names = metadata.metadataFormatNames
        for (name in names) {
            println("")
            println("-> Format name: ${name}" )
            //printImageIOMetadata(metadata.getAsTree(name))
        }
    }
    input.close()
}

void printImageIOMetadata(Node node, int level = 0) {
    print("    " * level)
    print("<${node.nodeName}")
    NamedNodeMap map = node.attributes
    if (map) {
        for (i in 0..map.length)
        while(child) {
             printImageIOMetadata(child, level + 1)
             child = child.nextSibling
        }
        print("    " * level)
        println("")
    }
    else {
        println(" />")
    }
    
}



// ---- Using Sanselan ------------------------------------------------
void printSanselan(File file) {
    //        get all metadata stored in EXIF format (ie. from JPEG or TIFF).
    //            org.w3c.dom.Node node = Sanselan.getMetadataObsolete(imageBytes);
    IImageMetadata metadata = Sanselan.getMetadata(file)
    if (!metadata) {
        println("\tNo image metadata was found")
        return
    }
    
    if (metadata instanceof JpegImageMetadata) {
        printSanselanJpegMetadata(metadata)
    }
    else {
        printSanselanOtherMetadata(metadata)
    }
}

void printSanselanJpegMetadata(JpegImageMetadata metadata) {
    JpegImageMetadata jpegMetadata = metadata

    // Jpeg EXIF metadata is stored in a TIFF-based directory structure
    // and is identified with TIFF tags.
    // Here we look for the "x resolution" tag, but
    // we could just as easily search for any other tag.
    //
    // see the TiffConstants file for a list of TIFF tags.

    // print out various interesting EXIF tags.
    println("  -- Standard EXIF Tags")
    printTagValue(jpegMetadata, TiffConstants.TIFF_TAG_XRESOLUTION)
    printTagValue(jpegMetadata, TiffConstants.TIFF_TAG_DATE_TIME)
    printTagValue(jpegMetadata, TiffConstants.EXIF_TAG_DATE_TIME_ORIGINAL)
    printTagValue(jpegMetadata, TiffConstants.EXIF_TAG_CREATE_DATE)
    printTagValue(jpegMetadata, TiffConstants.EXIF_TAG_ISO)
    printTagValue(jpegMetadata, TiffConstants.EXIF_TAG_SHUTTER_SPEED_VALUE)
    printTagValue(jpegMetadata, TiffConstants.EXIF_TAG_APERTURE_VALUE)
    printTagValue(jpegMetadata, TiffConstants.EXIF_TAG_BRIGHTNESS_VALUE)

    // simple interface to GPS data
    TiffImageMetadata exifMetadata = jpegMetadata.exif
    println("  -- GPS Info (using ${exifMetadata.getClass().getName()})")
    if (exifMetadata) {
        TiffImageMetadata.GPSInfo gpsInfo = exifMetadata.getGPS()
        if (gpsInfo) {
            double longitude = gpsInfo.longitudeAsDegreesEast
            double latitude = gpsInfo.latitudeAsDegreesNorth

            println("        GPS Description: ${gpsInfo}")
            println("        GPS Longitude (Degrees East): ${longitude}")
            println("        GPS Latitude (Degrees North): ${latitude}")
            
        }
        def tags = GPSTagConstants.ALL_GPS_TAGS
        tags.each { tag ->
            def field = exifMetadata.findField(tag)
            if (field) {
                def item = new TiffImageMetadata.Item(field)
                println("        ${tag.description}${item.text}")
            }
        }
        /*println("  -- All Tiff Info (using ${exifMetadata.getClass().getName()})")
        def directories = exifMetadata.directories.sort { it.toString() }
        directories.each { dir ->
            def fields = dir.allFields.sort { it.tagInfo.description }
            fields.each { field ->
                def item = new TiffImageMetadata.Item(field)
                println("        ${dir} -> ${field.tagInfo.description}${item.text}") 
            }
        } */
    }

    println("  -- All EXIF info (using ${metadata.getClass().getName()})")
    def items = jpegMetadata.items.sort { it.keyword } // List
    items.each { item ->
       println("        ${item}")   
    }
    println("")
}

void printSanselanOtherMetadata(IImageMetadata metadata) {
    println("  -- All image metadata info (using ${metadata.getClass().getName()})")
    def items = metadata.items.sort { it.keyword } // List
    items.each { item ->
       println("        ${item}")   
    }
    println("")
}

void printSanselanXMP(File file) {
    String xml = Sanselan.getXmpXml(file)
    if (xml) {
        println(xml)
    }
    println("")
}

void printTagValue(JpegImageMetadata jpegMetadata, TagInfo tagInfo) {
    TiffField field = jpegMetadata.findEXIFValue(tagInfo);
    if (field == null) {
        println("        (${tagInfo.name} not found.)")
    }
    else {
        println("        ${tagInfo.name}: ${field.valueDescription}")
    }
}
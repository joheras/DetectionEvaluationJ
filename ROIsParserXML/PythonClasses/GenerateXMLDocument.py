from lxml import etree

def generateXMLDocument(rois):
    if isAllSameROIType(rois):
        xml=etree.Element("ROIs")
        for i in range(len(rois)):
            xml.append(rois[i].generateXML())
        return xml
    else:
        return Exception.message("Error. Not all regions are the same")

def isAllSameROIType(rois):
    typeRoiToCompare = type(rois[0])

    for i in range(len(rois)):
        if type(rois[i])!=typeRoiToCompare:
            return False

    return True
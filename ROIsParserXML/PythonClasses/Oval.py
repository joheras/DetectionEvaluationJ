from lxml import etree
from ROI import ROI


class Oval(ROI):

    def __init__(self,x,y,height, width):
        ROI.__init__(self,x,y)
        self.height=height
        self.width= width


    def getHeight(self):
        return self.height

    def getWidth(self):
        return self.width


    def generateXML(self):
        oval = etree.Element("oval")
        point = etree.Element("point")
        point.append(etree.Element("x"))
        point.append(etree.Element("y"))
        point[0].text =str(self.getX())
        point[1].text = str(self.getY())
        oval.append(point)
        oval.append(etree.Element("height"))
        oval.append(etree.Element("width"))
        oval[1].text = str(self.getHeight())
        oval[2].text = str(self.getWidth())
        return oval



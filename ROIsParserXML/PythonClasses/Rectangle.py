from lxml import etree
from ROI import ROI


class Rectangle(ROI):

    def __init__(self,x,y,height, width):
        ROI.__init__(self,x,y)
        self.height=height
        self.width= width


    def getHeight(self):
        return self.height

    def getWidth(self):
        return self.width


    def generateXML(self):
        rectangle = etree.Element("rectangle")
        point = etree.Element("point")
        point.append(etree.Element("x"))
        point.append(etree.Element("y"))
        point[0].text = str(self.getX())
        point[1].text = str(self.getY())
        rectangle.append(point)
        rectangle.append(etree.Element("height"))
        rectangle.append(etree.Element("width"))
        rectangle[1].text = str(self.getHeight())
        rectangle[2].text = str(self.getWidth())
        return rectangle



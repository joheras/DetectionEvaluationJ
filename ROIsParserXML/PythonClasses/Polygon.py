from lxml import etree
from ROI import ROI

class Polygon(ROI):

    def __init__(self,x,y):
        ROI.__init__(self,x,y)

    def getXs(self):
        return self.getX()

    def getYs(self):
        return self.getY()

    def getXPos(self, position):
        return self.getXs()[position]

    def getYPos(self, position):
        return self.getYs()[position]

    def generateXML(self):
        polygon = etree.Element("polygon")
        for i in range(len(self.getXs())):
            point = etree.Element("point")
            point.append(etree.Element("x"))
            point.append(etree.Element("y"))
            point[0].text = str(self.getXPos(i))
            point[1].text = str(self.getYPos(i))
            polygon.append(point)
        return polygon



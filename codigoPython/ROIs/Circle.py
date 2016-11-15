from lxml import etree
from ROI import ROI


class Circle(ROI):

    def __init__(self,x,y,rat):
        ROI.__init__(self,x,y)
        self.ratio=rat


    def getRatio(self):
        return self.ratio


    def generateXML(self):
        circle = etree.Element("circle")
        point = etree.Element("point")
        point.append(etree.Element("x"))
        point.append(etree.Element("y"))
        point[0].text = str(self.getX())
        point[1].text = str(self.getY())
        circle.append(point)
        circle.append(etree.Element("ratio"))
        circle[1].text = str(self.getRatio())
        return circle



from lxml import etree
from ROI import ROI


class Point(ROI):

    def __init__(self,x,y):
        ROI.__init__(self,x,y)




    def generateXML(self):
        point = etree.Element("point")
        point.append(etree.Element("x"))
        point.append(etree.Element("y"))
        point[0].text = str(self.getX())
        point[1].text = str(self.getY())

        return point


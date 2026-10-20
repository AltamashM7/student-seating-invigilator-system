package com.examplanner.service;

import com.examplanner.dao.ReportDao.Report;
import java.io.*;
import java.nio.charset.*;
import java.text.Normalizer;
import java.util.*;

/** Plain, paginated PDF tables written with fundamental file/string operations. */
public class PdfReport {
    private static final Charset TEXT=Charset.forName("windows-1252");
    private static final double WIDTH=842,HEIGHT=595,LEFT=36,TABLE_WIDTH=770;
    public byte[] create(Report report) throws IOException {
        if(report.rows().size()>20000)throw new IllegalArgumentException("Narrow the report by examination for PDF, or download CSV");
        int cols=report.headings().size();double[] widths=new double[cols];double total=0;
        for(int i=0;i<cols;i++){String heading=report.headings().get(i);widths[i]=heading.equals("Student")||heading.equals("Faculty")||heading.equals("Subject")?25:heading.equals("Roll number")?19:heading.contains("status")||heading.equals("Access required")?15:heading.equals("Time")?15:heading.equals("Slot")?6:13;total+=widths[i];}
        for(int i=0;i<cols;i++)widths[i]=TABLE_WIDTH*widths[i]/total;
        List<StringBuilder> pages=new ArrayList<>();StringBuilder page=header(report,1,widths);pages.add(page);double y=HEIGHT-112;
        List<List<String>> data=report.rows().isEmpty()?List.of(Collections.nCopies(cols,"")):report.rows();
        int index=0;
        for(List<String> row:data){
            List<List<String>> wrapped=new ArrayList<>();int lines=1;
            for(int i=0;i<cols;i++){var cell=wrap(row.get(i),Math.max(4,(int)((widths[i]-10)/5.1)));wrapped.add(cell);lines=Math.max(lines,cell.size());}
            double height=lines*11+10;
            if(y-height<40){page=header(report,pages.size()+1,widths);pages.add(page);y=HEIGHT-112;}
            if(index++%2==0)page.append("0.96 0.97 0.98 rg ").append(f(LEFT)).append(' ').append(f(y-height)).append(' ').append(f(TABLE_WIDTH)).append(' ').append(f(height)).append(" re f\n");
            double x=LEFT;
            for(int i=0;i<cols;i++){int n=0;for(String line:wrapped.get(i))text(page,"F3",8.5,x+5,y-13-(n++)*11,line,"0.12 0.18 0.23");x+=widths[i];}
            y-=height;page.append("0.86 0.89 0.91 RG 0.4 w ").append(f(LEFT)).append(' ').append(f(y)).append(" m ").append(f(LEFT+TABLE_WIDTH)).append(' ').append(f(y)).append(" l S\n");
        }
        if(report.rows().isEmpty())text(pages.get(0),"F1",10,LEFT+5,HEIGHT-132,"No matching records.","0.25 0.3 0.35");
        List<byte[]> objects=new ArrayList<>();objects.add(ascii("<< /Type /Catalog /Pages 2 0 R >>"));objects.add(null);
        objects.add(ascii("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"));
        objects.add(ascii("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>"));
        objects.add(ascii("<< /Type /Font /Subtype /Type1 /BaseFont /Courier /Encoding /WinAnsiEncoding >>"));
        StringBuilder kids=new StringBuilder();
        for(int i=0;i<pages.size();i++){
            var content=pages.get(i);text(content,"F1",8,LEFT,22,"Exam Planner / "+(i+1)+" of "+pages.size(),"0.4 0.45 0.5");
            int pageId=objects.size()+1,streamId=pageId+1;kids.append(pageId).append(" 0 R ");
            objects.add(ascii("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 "+WIDTH+" "+HEIGHT+"] /Resources << /Font << /F1 3 0 R /F2 4 0 R /F3 5 0 R >> >> /Contents "+streamId+" 0 R >>"));
            byte[] body=content.toString().getBytes(TEXT);ByteArrayOutputStream stream=new ByteArrayOutputStream();stream.write(ascii("<< /Length "+body.length+" >>\nstream\n"));stream.write(body);stream.write(ascii("\nendstream"));objects.add(stream.toByteArray());
        }
        objects.set(1,ascii("<< /Type /Pages /Kids ["+kids+"] /Count "+pages.size()+" >>"));
        ByteArrayOutputStream output=new ByteArrayOutputStream();output.write(ascii("%PDF-1.4\n"));List<Integer> offsets=new ArrayList<>();offsets.add(0);
        for(int i=0;i<objects.size();i++){offsets.add(output.size());output.write(ascii((i+1)+" 0 obj\n"));output.write(objects.get(i));output.write(ascii("\nendobj\n"));}
        int xref=output.size();output.write(ascii("xref\n0 "+offsets.size()+"\n0000000000 65535 f \n"));
        for(int i=1;i<offsets.size();i++)output.write(ascii(String.format(Locale.ROOT,"%010d 00000 n \n",offsets.get(i))));
        output.write(ascii("trailer\n<< /Size "+offsets.size()+" /Root 1 0 R >>\nstartxref\n"+xref+"\n%%EOF\n"));return output.toByteArray();
    }
    private StringBuilder header(Report report,int page,double[] widths){
        var out=new StringBuilder();text(out,"F2",17,LEFT,HEIGHT-36,report.title(),"0.12 0.21 0.27");
        text(out,"F1",9,LEFT,HEIGHT-54,report.scope(),"0.3 0.4 0.45");
        out.append("0.12 0.21 0.27 rg ").append(f(LEFT)).append(' ').append(f(HEIGHT-110)).append(' ').append(f(TABLE_WIDTH)).append(" 40 re f\n");
        double x=LEFT;
        for(int i=0;i<widths.length;i++){int line=0;for(String part:wrap(report.headings().get(i),Math.max(5,(int)((widths[i]-10)/5.5))))text(out,"F2",9,x+5,HEIGHT-85-(line++)*11,part,"1 1 1");x+=widths[i];}
        return out;
    }
    private static List<String> wrap(String value,int width){
        String safe=Normalizer.normalize(value.replace('\n',' ').replace('\r',' ').replace('\t',' '),Normalizer.Form.NFC);
        List<String> result=new ArrayList<>();while(safe.length()>width){int split=safe.lastIndexOf(' ',width);if(split<width/3)split=width;result.add(safe.substring(0,split));safe=safe.substring(split).stripLeading();}result.add(safe);return result;
    }
    private static void text(StringBuilder out,String font,double size,double x,double y,String text,String color){
        // Standard PDF fonts support Western text. CSV preserves all original Unicode characters.
        byte[] bytes=text.getBytes(TEXT);StringBuilder escaped=new StringBuilder();
        for(byte raw:bytes){int ch=raw&255;if(ch=='('||ch==')'||ch=='\\')escaped.append('\\').append((char)ch);else if(ch<32||ch>126)escaped.append(String.format(Locale.ROOT,"\\%03o",ch));else escaped.append((char)ch);}
        out.append("BT /").append(font).append(' ').append(f(size)).append(" Tf ").append(color).append(" rg ").append(f(x)).append(' ').append(f(y)).append(" Td (").append(escaped).append(") Tj ET\n");
    }
    private static String f(double value){return String.format(Locale.ROOT,"%.2f",value);}
    private static byte[] ascii(String text){return text.getBytes(StandardCharsets.US_ASCII);}
    public byte[] csv(Report report){
        StringBuilder out=new StringBuilder("\uFEFF");out.append(csvRow(report.headings()));for(var row:report.rows())out.append(csvRow(row));return out.toString().getBytes(StandardCharsets.UTF_8);
    }
    private String csvRow(List<String> cells){StringJoiner join=new StringJoiner(",");for(String value:cells){String safe=value,trimmed=safe.stripLeading();if(!trimmed.isEmpty()&&"=+@-".indexOf(trimmed.charAt(0))>=0)safe="'"+safe;join.add("\""+safe.replace("\"","\"\"")+"\"");}return join+"\r\n";}
}

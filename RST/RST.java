package dpt.phases;
import dpt.sharedobjects.*;
import dpt.sharedobjects.tracedb.PersonalTrace;
import dpt.sharedobjects.tracedb.TraceDatabase;
import dpt.sharedobjects.tracedb.Trajectory;
import dpt.sharedobjects.tracedb.raw.RawLocation;
import dpt.sharedobjects.tracedb.rs.RSEvent;
import dpt.sharedobjects.tracedb.rs.RSEvent_RealStart;
import dpt.sharedobjects.tracedb.rs.RSLocation;

public class RST {
    RSS rss;

    public TraceDatabase rsTransform_db(TraceDatabase origin_db, RSS rss){
        this.rss = rss;
        TraceDatabase rs_db = new TraceDatabase();
        for(int i=0;i<origin_db.getSize();i++){
            PersonalTrace origin_person_trace = origin_db.getPersonalTrace(i);
            PersonalTrace rs_person_trace= new PersonalTrace();
            for(int j=0;j<origin_person_trace.getSize();j++){
                Trajectory origin_traj = origin_person_trace.getTrajectory(j);
                transform_traj(rs_person_trace, origin_traj);
            }
            rs_db.addTrace(i, rs_person_trace);
        }
        return rs_db;
    }

    private void transform_traj(PersonalTrace rs_person_trace, Trajectory origin_traj){
        Trajectory rs_traj = new Trajectory();
        rs_traj.addEvent(new RSEvent_RealStart());

        int old_rs_id = 0;
        int current_rs_id = 0;  //从最小的rs开始

        int jump = -1; //是否呆在同一个网格中

        boolean flag = false; //之前步数是否都为0，判断是否需要更改rs
        RawLocation old_rs_location;
        RawLocation current_rs_location;

        if(origin_traj!=null&&origin_traj.getLength()>=1){
            current_rs_location = (RawLocation) origin_traj.getEvent(0).getLoc();
        }else{
            return;
        }

        for(int i=1; i<origin_traj.getLength();i++){
            old_rs_location = current_rs_location;
            current_rs_location = (RawLocation) origin_traj.getEvent(i).getLoc();
             old_rs_id = current_rs_id;
             if(jump==0){
                 flag = true;
             }else {
                 flag = false;
             }

             int ans[] = getRS(old_rs_location, current_rs_location, old_rs_id, flag);
             current_rs_id = ans[0];
             jump = ans[1];

             if(i==1){
                 old_rs_id = current_rs_id;
             }

             if(jump<=1){
                 if(current_rs_id==old_rs_id){
                     int x = rss.getRSbyID(current_rs_id).getIndex(old_rs_location).getX();
                     int y = rss.getRSbyID(current_rs_id).getIndex(old_rs_location).getY();
                     RSEvent rs_event = new RSEvent(x,y,current_rs_id);
                     rs_traj.addEvent(rs_event);
                 }else{
                     int x = rss.getRSbyID(old_rs_id).getIndex(old_rs_location).getX();
                     int y = rss.getRSbyID(old_rs_id).getIndex(old_rs_location).getY();
                     RSEvent rs_event = new RSEvent(x,y,old_rs_id);
                     rs_traj.addEvent(rs_event);

                     x = rss.getRSbyID(current_rs_id).getIndex(old_rs_location).getX();
                     y = rss.getRSbyID(current_rs_id).getIndex(old_rs_location).getY();
                     rs_event = new RSEvent(x,y,current_rs_id);
                     rs_traj.addEvent(rs_event);

                     rs_person_trace.addTraj(rs_traj);

                     rs_traj = new Trajectory();
                     x = rss.getRSbyID(current_rs_id).getIndex(old_rs_location).getX();
                     y = rss.getRSbyID(current_rs_id).getIndex(old_rs_location).getY();
                     rs_event = new RSEvent(x,y,current_rs_id);
                     rs_traj.addEvent(rs_event);
                     old_rs_id = current_rs_id;
                 }
             }else{
                 if(current_rs_id>old_rs_id){
                     int x = rss.getRSbyID(old_rs_id).getIndex(old_rs_location).getX();
                     int y = rss.getRSbyID(old_rs_id).getIndex(old_rs_location).getY();
                     RSEvent rs_event = new RSEvent(x,y,old_rs_id);
                     rs_traj.addEvent(rs_event);

                     x = rss.getRSbyID(current_rs_id).getIndex(old_rs_location).getX();
                     y = rss.getRSbyID(current_rs_id).getIndex(old_rs_location).getY();
                     rs_event = new RSEvent(x,y,current_rs_id);
                     rs_traj.addEvent(rs_event);

                     rs_person_trace.addTraj(rs_traj);
                 }

                 old_rs_id = current_rs_id;
                 rs_traj = new Trajectory();
                 int x = rss.getRSbyID(current_rs_id).getIndex(old_rs_location).getX();
                 int y = rss.getRSbyID(current_rs_id).getIndex(old_rs_location).getY();
                 RSEvent rs_event = new RSEvent(x,y,current_rs_id);
                 rs_traj.addEvent(rs_event);

                 double x_different = (current_rs_location.getX()-old_rs_location.getX())/jump;
                 double y_different = (current_rs_location.getY()-old_rs_location.getY())/jump;

                 for(int j=0;j<jump;j++){
                     old_rs_location.setX(old_rs_location.getX()+x_different);
                     old_rs_location.setY(old_rs_location.getY()+y_different);

                     RSLocation temp_rs_locat = rss.getRSbyID(current_rs_id).getIndex(old_rs_location);

                     if(temp_rs_locat!=null){
                         x = temp_rs_locat.getX();
                         y = temp_rs_locat.getY();
                         rs_event = new RSEvent(x,y,current_rs_id);
                         rs_traj.addEvent(rs_event);
                     }
                 }
             }
        }
        // 增加结束点
        if(current_rs_location!=null){
            int x = rss.getRSbyID(current_rs_id).getIndex(current_rs_location).getX();
            int y = rss.getRSbyID(current_rs_id).getIndex(current_rs_location).getY();
            RSEvent rs_event = new RSEvent(x,y,current_rs_id);
            rs_traj.addEvent(rs_event);
            rs_traj.addEvent(new RSEvent_RealStart());
            rs_person_trace.addTraj(rs_traj);
        }

    }

    private int[] getRS(RawLocation location1, RawLocation location2, int old_rs_id, boolean flag){
        int ans[] = new int[2];
        int rss_size = rss.getSize();
        int current_rs_id = old_rs_id;
        RS current_rs = rss.getRSbyID(current_rs_id);

        RSLocation old_rs_locat = current_rs.getIndex(location1);
        RSLocation current_rs_locat = current_rs.getIndex(location2);

        int jump_size = jump_size(old_rs_locat, current_rs_locat);

        if(jump_size==1){
            ans[0] = current_rs_id;
            ans[1] = jump_size;
            return ans;
        }

        // 换到更小的rs不能一步到达，选择停留在当前rs，步数为0
        while((jump_size==0)&&(flag)&&(current_rs_id>0)){
            current_rs_id --;
            current_rs = rss.getRSbyID(current_rs_id);
            old_rs_locat = current_rs.getIndex(location1);
            current_rs_locat = current_rs.getIndex(location2);
            jump_size = jump_size(old_rs_locat, current_rs_locat);
            if(jump_size>1){
                ans[0] = current_rs_id+1;
                ans[1] = 0;
                return ans;
            }
        }

        if(jump_size<=1){
            ans[0] = current_rs_id;
            ans[1] = jump_size;
        }

        // 换到更大的rs
        while ((jump_size>1)&&(current_rs_id<(rss_size-1))){
            current_rs_id++;
            current_rs = rss.getRSbyID(current_rs_id);
            old_rs_locat = current_rs.getIndex(location1);
            current_rs_locat = current_rs.getIndex(location2);
            jump_size = jump_size(old_rs_locat, current_rs_locat);
            if(jump_size<=1){
                ans[0] = current_rs_id;
                ans[1] = jump_size;
                return ans;
            }
        }

        //default
        ans[0] = current_rs_id;
        ans[1] = jump_size;
        return ans;
    }

    private int jump_size(RSLocation location1, RSLocation location2){
        int temp = Math.max(Math.abs(location2.getX()-location1.getX()),Math.abs(location2.getY()-location1.getY()));
        return temp;
    }
}

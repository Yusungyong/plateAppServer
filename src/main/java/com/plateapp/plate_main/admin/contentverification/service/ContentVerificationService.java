package com.plateapp.plate_main.admin.contentverification.service;
import com.plateapp.plate_main.admin.audit.service.AdminAuditService;
import com.plateapp.plate_main.admin.common.AdminPageResponse;
import com.plateapp.plate_main.admin.contentverification.dto.ContentVerificationDtos.*;
import com.plateapp.plate_main.admin.contentverification.entity.*;
import com.plateapp.plate_main.admin.contentverification.repository.*;
import com.plateapp.plate_main.admin.security.AdminActor;
import com.plateapp.plate_main.common.error.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class ContentVerificationService {
 private static final Set<String> STATES=Set.of("pending","in_review","approved","rejected","changes_requested");
 private final ContentVerificationRepository repository;
 private final ContentVerificationHistoryRepository historyRepository;
 private final AdminAuditService auditService;
 @Transactional(readOnly=true)
 public AdminPageResponse<Response> list(int page,int size,String status,String targetType,Integer assignee,String keyword){
  if(page<0||size<1||size>100) throw invalid("Invalid page or size.");
  status=norm(status); if(status!=null&&!STATES.contains(status)) throw invalid("Unsupported status.");
  Page<ContentVerification> result=repository.search(status,norm(targetType),assignee,trim(keyword),
    PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt","id")));
  return AdminPageResponse.from(result.map(this::map));
 }
 @Transactional(readOnly=true) public Response detail(Long id){return map(find(id));}
 @Transactional(readOnly=true) public List<HistoryResponse> history(Long id){find(id); return historyRepository
  .findByVerificationIdOrderByCreatedAtDescIdDesc(id).stream().map(h->new HistoryResponse(h.getId(),h.getAction(),
   h.getPreviousStatus(),h.getNextStatus(),h.getActorUserId(),h.getAssigneeUserId(),h.getReason(),h.getCreatedAt())).toList();}
 @Transactional public Response assign(Long id,AssigneeRequest cmd,AdminActor actor,HttpServletRequest req){
  ContentVerification v=findVersion(id,cmd.version()); String prev=v.getStatus(); v.assign(cmd.assigneeUserId());
  v=repository.saveAndFlush(v); record(v,"ASSIGNED",prev,actor,null,req); return map(v);
 }
 @Transactional public Response decide(Long id,ActionRequest cmd,String action,AdminActor actor,HttpServletRequest req){
  ContentVerification v=findVersion(id,cmd.version()); String next=switch(action){case "APPROVED"->"approved";
   case "REJECTED"->"rejected"; case "CHANGES_REQUESTED"->"changes_requested"; default->throw invalid("Unsupported action.");};
  if(!Set.of("pending","in_review","changes_requested").contains(v.getStatus())) throw new AppException(ErrorCode.COMMON_CONFLICT,"Verification is already finalized.");
  String reason=trim(cmd.reason()); if(!"APPROVED".equals(action)&&reason==null) throw invalid("reason is required.");
  String prev=v.getStatus(); v.decide(next,reason); v=repository.saveAndFlush(v); record(v,action,prev,actor,reason,req); return map(v);
 }
 private void record(ContentVerification v,String action,String previous,AdminActor actor,String reason,HttpServletRequest req){
  historyRepository.save(ContentVerificationHistory.create(v.getId(),action,previous,v.getStatus(),actor.userId(),v.getAssigneeUserId(),reason));
  auditService.record(actor,"CONTENT_VERIFICATION_"+action,"CONTENT_VERIFICATION",v.getId(),Map.of("status",previous),
   Map.of("status",v.getStatus(),"version",v.getVersion()),null,reason,req);
 }
 private ContentVerification find(Long id){return repository.findById(id).orElseThrow(()->new AppException(ErrorCode.COMMON_NOT_FOUND));}
 private ContentVerification findVersion(Long id,Long version){ContentVerification v=find(id);if(!Objects.equals(version,v.getVersion()))throw new AppException(ErrorCode.COMMON_CONFLICT);return v;}
 private Response map(ContentVerification v){return new Response(v.getId(),v.getTargetType(),v.getTargetId(),v.getStatus(),v.getRequesterUserId(),v.getAssigneeUserId(),v.getReviewReason(),v.getCreatedAt(),v.getUpdatedAt(),v.getVersion());}
 private String trim(String s){return s==null||s.isBlank()?null:s.trim();} private String norm(String s){String v=trim(s);return v==null?null:v.toLowerCase(Locale.ROOT);}
 private AppException invalid(String m){return new AppException(ErrorCode.COMMON_INVALID_INPUT,m);}
}

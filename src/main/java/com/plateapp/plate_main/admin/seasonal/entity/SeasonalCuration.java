package com.plateapp.plate_main.admin.seasonal.entity;
import jakarta.persistence.*;import java.time.*;import java.util.*;import java.util.stream.Collectors;import lombok.Getter;
@Entity @Table(name="seasonal_curations") @Getter
public class SeasonalCuration {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)private Long id;
 @Column(nullable=false,length=150)private String title;@Column(columnDefinition="text")private String description;
 @Column(nullable=false,length=30)private String status;@Column(name="display_order",nullable=false)private Integer displayOrder;
 @Column(name="starts_at")private OffsetDateTime startsAt;@Column(name="ends_at")private OffsetDateTime endsAt;
 @Column(name="store_ids",columnDefinition="text")private String storeIds;@Column(name="menu_ids",columnDefinition="text")private String menuIds;
 @Column(name="created_by",nullable=false)private Integer createdBy;@Column(name="updated_by",nullable=false)private Integer updatedBy;
 @Column(name="created_at",nullable=false)private OffsetDateTime createdAt;@Column(name="updated_at",nullable=false)private OffsetDateTime updatedAt;
 @Version private Long version;protected SeasonalCuration(){}
 public static SeasonalCuration create(String title,String description,Integer order,OffsetDateTime starts,OffsetDateTime ends,List<Long> stores,List<Long> menus,Integer actor){SeasonalCuration c=new SeasonalCuration();c.status="DRAFT";c.createdBy=actor;c.createdAt=OffsetDateTime.now(ZoneOffset.UTC);c.update(title,description,order,starts,ends,stores,menus,actor);return c;}
 public void update(String title,String description,Integer order,OffsetDateTime starts,OffsetDateTime ends,List<Long> stores,List<Long> menus,Integer actor){this.title=title;this.description=description;this.displayOrder=order==null?0:order;this.startsAt=starts;this.endsAt=ends;this.storeIds=join(stores);this.menuIds=join(menus);this.updatedBy=actor;this.updatedAt=OffsetDateTime.now(ZoneOffset.UTC);}
 public void publish(OffsetDateTime now){this.status=startsAt!=null&&startsAt.isAfter(now)?"SCHEDULED":"PUBLISHED";this.updatedAt=now;}
 public void reorder(int order,Integer actor){this.displayOrder=order;this.updatedBy=actor;this.updatedAt=OffsetDateTime.now(ZoneOffset.UTC);}
 public List<Long> storeIdList(){return parse(storeIds);}public List<Long> menuIdList(){return parse(menuIds);}
 private static String join(List<Long> ids){return ids==null||ids.isEmpty()?null:ids.stream().distinct().map(String::valueOf).collect(Collectors.joining(","));}
 private static List<Long> parse(String value){if(value==null||value.isBlank())return List.of();return Arrays.stream(value.split(",")).map(Long::valueOf).toList();}
}

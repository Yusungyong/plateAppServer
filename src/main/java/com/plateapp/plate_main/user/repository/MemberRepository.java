package com.plateapp.plate_main.user.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import com.plateapp.plate_main.user.entity.Fp100User;

public interface MemberRepository extends JpaRepository<Fp100User, String> {
  List<Fp100User> findByUsernameIn(Collection<String> usernames);

  @Query("""
      select u
      from Fp100User u
      where
        lower(u.username) like lower(concat('%', :kw, '%'))
        or lower(u.nickName) like lower(concat('%', :kw, '%'))
        or lower(u.activeRegion) like lower(concat('%', :kw, '%'))
      """)
  List<Fp100User> searchByKeyword(@Param("kw") String keyword, Pageable pageable);
}

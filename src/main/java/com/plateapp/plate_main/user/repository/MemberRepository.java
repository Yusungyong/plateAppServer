package com.plateapp.plate_main.user.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plateapp.plate_main.user.entity.Fp100User;

public interface MemberRepository extends JpaRepository<Fp100User, String> {
  List<Fp100User> findByUsernameIn(Collection<String> usernames);
}

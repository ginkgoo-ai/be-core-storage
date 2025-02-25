package com.ginkgooai.repository;

import com.ginkgooai.domain.CloudFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CloudFileRepository extends JpaRepository<CloudFile, String> {

}
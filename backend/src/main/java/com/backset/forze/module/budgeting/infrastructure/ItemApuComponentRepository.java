package com.backset.forze.module.budgeting.infrastructure;

import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.budget.ItemApuComponent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemApuComponentRepository extends JpaRepository<ItemApuComponent, UUID> {

	List<ItemApuComponent> findByItemApuIdOrderByPosition(UUID itemApuId);
}

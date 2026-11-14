package com.cosmocats.marketplace.infrastructure.persistence.jpa;

import com.cosmocats.marketplace.domain.model.OrderStatus;
import com.cosmocats.marketplace.infrastructure.persistence.entity.OrderItemEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OrderItemJpaRepository extends JpaRepository<OrderItemEntity, Long> {

    @Query("""
            select p.publicId as productId,
                   p.productName as productName,
                   sum(oi.quantity) as totalQuantity
            from OrderItemEntity oi
              join oi.product p
              join oi.order o
            where o.status in :statuses
            group by p.publicId, p.productName
            order by sum(oi.quantity) desc, p.productName asc
            """)
    List<ProductSalesProjection> findTopSellingProducts(
            @Param("statuses") Collection<OrderStatus> statuses, Pageable pageable);
}

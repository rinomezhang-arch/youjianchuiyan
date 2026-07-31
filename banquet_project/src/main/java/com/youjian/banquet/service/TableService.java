/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.dto.TableDTO
 *  com.youjian.banquet.dto.TableReorderDTO
 *  com.youjian.banquet.entity.BookingTable
 *  com.youjian.banquet.entity.TableMaster
 *  com.youjian.banquet.repository.BookingTableRepository
 *  com.youjian.banquet.repository.TableMasterRepository
 *  com.youjian.banquet.service.TableService
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.youjian.banquet.service;

import com.youjian.banquet.dto.TableDTO;
import com.youjian.banquet.dto.TableReorderDTO;
import com.youjian.banquet.entity.BookingTable;
import com.youjian.banquet.entity.TableMaster;
import com.youjian.banquet.repository.BookingTableRepository;
import com.youjian.banquet.repository.TableMasterRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TableService {
    @Autowired
    private TableMasterRepository tableMasterRepository;
    @Autowired
    private BookingTableRepository bookingTableRepository;

    public List<TableDTO> getAllTables(String storeId) {
        return this.tableMasterRepository.findByStoreIdOrderBySortOrderAsc(Long.valueOf(Long.parseLong(storeId))).stream().map(arg_0 -> this.toDTO(arg_0)).collect(Collectors.toList());
    }

    public List<TableDTO> getTablesByStatus(String storeId, String status) {
        return this.tableMasterRepository.findByStoreIdAndTableStatus(Long.valueOf(Long.parseLong(storeId)), status).stream().map(arg_0 -> this.toDTO(arg_0)).collect(Collectors.toList());
    }

    public TableDTO getTable(String tableId, String storeId) {
        return this.tableMasterRepository.findByTableIdAndStoreId(Integer.valueOf(Integer.parseInt(tableId)), Long.valueOf(Long.parseLong(storeId))).map(arg_0 -> this.toDTO(arg_0)).orElseThrow(() -> new IllegalArgumentException("Table not found: " + tableId));
    }

    @Transactional
    public TableDTO createTable(TableDTO dto) {
        TableMaster table = new TableMaster();
        table.setStoreId(Long.valueOf(Long.parseLong(dto.getStoreId())));
        table.setTableNumber(dto.getTableName() != null ? dto.getTableName() : dto.getTableId());
        table.setTableName(dto.getTableName());
        table.setTableCapacity(dto.getCapacity());
        table.setTableArea(dto.getArea());
        table.setTableStatus(dto.getStatus() != null ? dto.getStatus() : "available");
        table.setSortOrder(Integer.valueOf(dto.getSortOrder() != null ? dto.getSortOrder() : 0));
        table.setIsActive(Integer.valueOf(1));
        table.setRemark(dto.getNotes());
        this.tableMasterRepository.save(table);
        return this.toDTO(table);
    }

    @Transactional
    public TableDTO updateTable(String tableId, String storeId, TableDTO dto) {
        TableMaster table = (TableMaster)this.tableMasterRepository.findByTableIdAndStoreId(Integer.valueOf(Integer.parseInt(tableId)), Long.valueOf(Long.parseLong(storeId))).orElseThrow(() -> new IllegalArgumentException("Table not found: " + tableId));
        if (dto.getTableName() != null) {
            table.setTableName(dto.getTableName());
        }
        if (dto.getCapacity() != null) {
            table.setTableCapacity(dto.getCapacity());
        }
        if (dto.getArea() != null) {
            table.setTableArea(dto.getArea());
        }
        if (dto.getStatus() != null) {
            table.setTableStatus(dto.getStatus());
        }
        if (dto.getSortOrder() != null) {
            table.setSortOrder(dto.getSortOrder());
        }
        if (dto.getNotes() != null) {
            table.setRemark(dto.getNotes());
        }
        this.tableMasterRepository.save(table);
        return this.toDTO(table);
    }

    @Transactional
    public void deleteTable(String tableId, String storeId) {
        this.tableMasterRepository.deleteByTableIdAndStoreId(Integer.valueOf(Integer.parseInt(tableId)), Long.valueOf(Long.parseLong(storeId)));
    }

    @Transactional
    public void reorderTables(TableReorderDTO dto) {
        List tableIds = dto.getTableIds();
        for (int i = 0; i < tableIds.size(); ++i) {
            int order = i + 1;
            String tableId = (String)tableIds.get(i);
            this.tableMasterRepository.findByTableIdAndStoreId(Integer.valueOf(Integer.parseInt(tableId)), Long.valueOf(Long.parseLong(dto.getStoreId()))).ifPresent(table -> {
                table.setSortOrder(Integer.valueOf(order));
                this.tableMasterRepository.save(table);
            });
        }
    }

    @Transactional
    public void swapBookingTable(String storeId, String bookingId, String tableId1, String tableId2) {
        List<BookingTable> bt1 = this.bookingTableRepository.findByBookingIdAndStoreId(bookingId, Long.valueOf(Long.parseLong(storeId)));
        bt1.stream().filter(bt -> String.valueOf(bt.getTableId()).equals(tableId1)).findFirst().ifPresent(bt -> {
            this.bookingTableRepository.delete(bt);
            BookingTable newBt = new BookingTable();
            newBt.setBookingId(bookingId);
            newBt.setStoreId(Long.valueOf(Long.parseLong(storeId)));
            newBt.setTableId(Integer.valueOf(Integer.parseInt(tableId2)));
            this.bookingTableRepository.save(newBt);
        });
    }

    private TableDTO toDTO(TableMaster e) {
        TableDTO dto = new TableDTO();
        dto.setTableId(String.valueOf(e.getTableId()));
        dto.setStoreId(String.valueOf(e.getStoreId()));
        dto.setTableName(e.getTableName());
        dto.setCapacity(e.getTableCapacity());
        dto.setArea(e.getTableArea());
        dto.setStatus(e.getTableStatus());
        dto.setSortOrder(e.getSortOrder());
        dto.setNotes(e.getRemark());
        return dto;
    }
}


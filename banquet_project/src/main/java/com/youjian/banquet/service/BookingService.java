package com.youjian.banquet.service;

import com.youjian.banquet.dto.BookingDTO;
import com.youjian.banquet.dto.BookingDishDTO;
import com.youjian.banquet.entity.BookingDishDetail;
import com.youjian.banquet.entity.BookingMaster;
import com.youjian.banquet.entity.BookingTable;
import com.youjian.banquet.repository.BookingDishDetailRepository;
import com.youjian.banquet.repository.BookingMasterRepository;
import com.youjian.banquet.repository.BookingTableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingService {

    @Autowired
    private BookingMasterRepository bookingMasterRepository;

    @Autowired
    private BookingTableRepository bookingTableRepository;

    @Autowired
    private BookingDishDetailRepository bookingDishDetailRepository;

    public List<BookingDTO> getAllBookings(String storeId) {
        return bookingMasterRepository.findByStoreId(Long.parseLong(storeId)).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<BookingDTO> getBookingsByDate(String storeId, LocalDate date) {
        return bookingMasterRepository.findByStoreIdAndBookingDate(Long.parseLong(storeId), date).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<BookingDTO> getBookingsByStatus(String storeId, String status) {
        return bookingMasterRepository.findByStoreIdAndBookingStatus(Long.parseLong(storeId), status).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public BookingDTO getBooking(String bookingId, String storeId) {
        return bookingMasterRepository.findByBookingIdAndStoreId(bookingId, Long.parseLong(storeId))
                .map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
    }

    @Transactional
    public BookingDTO createBooking(BookingDTO dto) {
        BookingMaster booking = new BookingMaster();
        Long storeId = Long.parseLong(dto.getStoreId() != null ? dto.getStoreId() : "1");

        String bookingId = dto.getBookingId();
        if (bookingId == null || bookingId.isEmpty()) {
            bookingId = generateBookingId(storeId);
        }
        booking.setBookingId(bookingId);
        booking.setStoreId(storeId);

        if (dto.getCustomerId() != null) {
            try {
                booking.setCustomerId(Integer.parseInt(dto.getCustomerId()));
            } catch (Exception ignored) {}
        }
        booking.setCustomerName(dto.getCustomerName());
        booking.setCustomerPhone(dto.getCustomerPhone());
        booking.setBookingDate(dto.getBookingDate());
        booking.setBookingTime(dto.getBookingTime());
        booking.setBookingType(dto.getBookingType() != null ? dto.getBookingType() : "normal");
        booking.setGuestCount(dto.getGuestCount() != null ? dto.getGuestCount() : 0);
        booking.setTableCount(dto.getTableCount() != null ? dto.getTableCount() : 1);
        booking.setSpareTables(dto.getSpareTables() != null ? dto.getSpareTables() : 0);
        booking.setGuestPerTable(dto.getGuestPerTable() != null ? dto.getGuestPerTable() : 10);
        booking.setOccasionType(dto.getOccasionType() != null ? dto.getOccasionType() : dto.getOccasion());
        booking.setBanquetName(dto.getBanquetName());
        booking.setPackageId(dto.getPackageId());
        booking.setPackageName(dto.getPackageName());
        booking.setTotalAmount(dto.getTotalAmount() != null ? dto.getTotalAmount() : BigDecimal.ZERO);
        booking.setFinalAmount(dto.getFinalAmount() != null ? dto.getFinalAmount() : BigDecimal.ZERO);
        booking.setDepositAmount(dto.getDepositAmount() != null ? dto.getDepositAmount() : BigDecimal.ZERO);

        String status = dto.getBookingStatus() != null ? dto.getBookingStatus()
                : (dto.getStatus() != null ? dto.getStatus() : "confirmed");
        booking.setBookingStatus(status);
        booking.setStatus(status);

        booking.setPaymentStatus(dto.getPaymentStatus() != null ? dto.getPaymentStatus() : "unpaid");
        booking.setRemark(dto.getRemark() != null ? dto.getRemark() : dto.getNotes());
        booking.setSpecialRequest(dto.getSpecialRequest());

        booking.setStaffId(dto.getStaffId());
        booking.setStaffName(dto.getStaffName());

        if (dto.getBookingNo() != null && !dto.getBookingNo().isEmpty()) {
            booking.setBookingNo(dto.getBookingNo());
        } else {
            booking.setBookingNo(bookingId);
        }

        if (dto.getCreatedAt() != null) {
            booking.setCreatedAt(dto.getCreatedAt());
        } else {
            booking.setCreatedAt(LocalDateTime.now());
        }
        booking.setUpdatedAt(LocalDateTime.now());

        bookingMasterRepository.save(booking);

        if (dto.getTableIds() != null) {
            int idx = 0;
            for (String tableId : dto.getTableIds()) {
                BookingTable bt = new BookingTable();
                bt.setBookingId(booking.getBookingId());
                bt.setStoreId(booking.getStoreId());
                try {
                    bt.setTableId(Integer.parseInt(tableId));
                } catch (Exception ignored) {
                    continue;
                }
                if (dto.getTableNames() != null && idx < dto.getTableNames().size()) {
                    bt.setTableName(dto.getTableNames().get(idx));
                }
                bt.setBookingDate(booking.getBookingDate());
                bt.setBookingTime(booking.getBookingTime());
                bt.setGuestCount(booking.getGuestCount());
                bookingTableRepository.save(bt);
                idx++;
            }
        }

        if (dto.getDishes() != null) {
            for (BookingDishDTO dishDTO : dto.getDishes()) {
                BookingDishDetail dish = new BookingDishDetail();
                dish.setBookingId(booking.getBookingId());
                dish.setStoreId(booking.getStoreId());
                dish.setDishId(dishDTO.getDishId());
                dish.setDishName(dishDTO.getDishName());
                dish.setDishQuantity(dishDTO.getQuantity());
                dish.setUnitPrice(dishDTO.getUnitPrice());
                dish.setSubtotal(dishDTO.getSubtotal());
                dish.setDishNote(dishDTO.getNotes());
                bookingDishDetailRepository.save(dish);
            }
        }

        return toDTO(booking);
    }

    @Transactional
    public BookingDTO updateBooking(String bookingId, String storeId, BookingDTO dto) {
        BookingMaster booking = bookingMasterRepository.findByBookingIdAndStoreId(bookingId, Long.parseLong(storeId))
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (dto.getCustomerName() != null) booking.setCustomerName(dto.getCustomerName());
        if (dto.getCustomerPhone() != null) booking.setCustomerPhone(dto.getCustomerPhone());
        if (dto.getBookingDate() != null) booking.setBookingDate(dto.getBookingDate());
        if (dto.getBookingTime() != null) booking.setBookingTime(dto.getBookingTime());
        if (dto.getBookingType() != null) booking.setBookingType(dto.getBookingType());
        if (dto.getGuestCount() != null) booking.setGuestCount(dto.getGuestCount());
        if (dto.getTableCount() != null) booking.setTableCount(dto.getTableCount());
        if (dto.getSpareTables() != null) booking.setSpareTables(dto.getSpareTables());
        if (dto.getGuestPerTable() != null) booking.setGuestPerTable(dto.getGuestPerTable());
        if (dto.getOccasionType() != null) booking.setOccasionType(dto.getOccasionType());
        if (dto.getOccasion() != null && booking.getOccasionType() == null) booking.setOccasionType(dto.getOccasion());
        if (dto.getBanquetName() != null) booking.setBanquetName(dto.getBanquetName());
        if (dto.getPackageId() != null) booking.setPackageId(dto.getPackageId());
        if (dto.getPackageName() != null) booking.setPackageName(dto.getPackageName());
        if (dto.getTotalAmount() != null) booking.setTotalAmount(dto.getTotalAmount());
        if (dto.getFinalAmount() != null) booking.setFinalAmount(dto.getFinalAmount());
        if (dto.getDepositAmount() != null) booking.setDepositAmount(dto.getDepositAmount());
        if (dto.getBookingStatus() != null) {
            booking.setBookingStatus(dto.getBookingStatus());
            booking.setStatus(dto.getBookingStatus());
        }
        if (dto.getStatus() != null && booking.getBookingStatus() == null) {
            booking.setBookingStatus(dto.getStatus());
            booking.setStatus(dto.getStatus());
        }
        if (dto.getPaymentStatus() != null) booking.setPaymentStatus(dto.getPaymentStatus());
        if (dto.getRemark() != null) booking.setRemark(dto.getRemark());
        if (dto.getNotes() != null && booking.getRemark() == null) booking.setRemark(dto.getNotes());
        if (dto.getSpecialRequest() != null) booking.setSpecialRequest(dto.getSpecialRequest());
        if (dto.getStaffId() != null) booking.setStaffId(dto.getStaffId());
        if (dto.getStaffName() != null) booking.setStaffName(dto.getStaffName());

        booking.setUpdatedAt(LocalDateTime.now());
        bookingMasterRepository.save(booking);

        if (dto.getTableIds() != null) {
            bookingTableRepository.deleteByBookingIdAndStoreId(bookingId, Long.parseLong(storeId));
            int idx = 0;
            for (String tableId : dto.getTableIds()) {
                BookingTable bt = new BookingTable();
                bt.setBookingId(bookingId);
                bt.setStoreId(Long.parseLong(storeId));
                try {
                    bt.setTableId(Integer.parseInt(tableId));
                } catch (Exception ignored) {
                    continue;
                }
                if (dto.getTableNames() != null && idx < dto.getTableNames().size()) {
                    bt.setTableName(dto.getTableNames().get(idx));
                }
                bookingTableRepository.save(bt);
                idx++;
            }
        }

        if (dto.getDishes() != null) {
            bookingDishDetailRepository.deleteByBookingIdAndStoreId(bookingId, Long.parseLong(storeId));
            for (BookingDishDTO dishDTO : dto.getDishes()) {
                BookingDishDetail dish = new BookingDishDetail();
                dish.setBookingId(bookingId);
                dish.setStoreId(Long.parseLong(storeId));
                dish.setDishId(dishDTO.getDishId());
                dish.setDishName(dishDTO.getDishName());
                dish.setDishQuantity(dishDTO.getQuantity());
                dish.setUnitPrice(dishDTO.getUnitPrice());
                dish.setSubtotal(dishDTO.getSubtotal());
                dish.setDishNote(dishDTO.getNotes());
                bookingDishDetailRepository.save(dish);
            }
        }

        return toDTO(booking);
    }

    @Transactional
    public void deleteBooking(String bookingId, String storeId) {
        bookingTableRepository.deleteByBookingIdAndStoreId(bookingId, Long.parseLong(storeId));
        bookingDishDetailRepository.deleteByBookingIdAndStoreId(bookingId, Long.parseLong(storeId));
        bookingMasterRepository.findByBookingIdAndStoreId(bookingId, Long.parseLong(storeId))
                .ifPresent(bookingMasterRepository::delete);
    }

    public List<BookingDTO> getBookingsByCustomer(String storeId, String customerId) {
        return bookingMasterRepository.findByStoreIdAndCustomerId(Long.parseLong(storeId), Integer.parseInt(customerId)).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public String generateBookingId(Long storeId) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "YJ" + dateStr;
        int seq = 1;
        try {
            List<BookingMaster> todayList = bookingMasterRepository
                    .findByStoreIdAndBookingIdStartingWith(storeId, prefix);
            if (todayList != null && !todayList.isEmpty()) {
                int maxSeq = 0;
                for (BookingMaster b : todayList) {
                    try {
                        String suffix = b.getBookingId().substring(prefix.length());
                        int num = Integer.parseInt(suffix);
                        if (num > maxSeq) maxSeq = num;
                    } catch (Exception ignored) {}
                }
                seq = maxSeq + 1;
            }
        } catch (Exception ignored) {}
        return prefix + String.format("%04d", seq);
    }

    private BookingDTO toDTO(BookingMaster e) {
        BookingDTO dto = new BookingDTO();
        dto.setBookingId(e.getBookingId());
        dto.setBookingNo(e.getBookingId());
        dto.setStoreId(String.valueOf(e.getStoreId()));
        dto.setCustomerId(e.getCustomerId() != null ? String.valueOf(e.getCustomerId()) : null);
        dto.setCustomerName(e.getCustomerName());
        dto.setCustomerPhone(e.getCustomerPhone());
        dto.setBookingDate(e.getBookingDate());
        dto.setBookingTime(e.getBookingTime());
        dto.setBookingType(e.getBookingType());
        dto.setGuestCount(e.getGuestCount());
        dto.setTableCount(e.getTableCount());
        dto.setSpareTables(e.getSpareTables());
        dto.setGuestPerTable(e.getGuestPerTable());
        dto.setOccasionType(e.getOccasionType());
        dto.setOccasion(e.getOccasionType());
        dto.setBanquetName(e.getBanquetName());
        dto.setPackageId(e.getPackageId());
        dto.setPackageName(e.getPackageName());
        dto.setTotalAmount(e.getTotalAmount());
        dto.setFinalAmount(e.getFinalAmount());
        dto.setDepositAmount(e.getDepositAmount());
        dto.setBookingStatus(e.getBookingStatus());
        dto.setStatus(e.getBookingStatus());
        dto.setPaymentStatus(e.getPaymentStatus());
        dto.setRemark(e.getRemark());
        dto.setNotes(e.getRemark());
        dto.setSpecialRequest(e.getSpecialRequest());
        dto.setStaffId(e.getStaffId());
        dto.setStaffName(e.getStaffName());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());
        dto.setCreatedBy(e.getStaffName());

        List<BookingTable> tables = bookingTableRepository.findByBookingIdAndStoreId(e.getBookingId(), e.getStoreId());
        List<String> tableIds = tables.stream()
                .map(bt -> bt.getTableId() != null ? String.valueOf(bt.getTableId()) : null)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        dto.setTableIds(tableIds);

        List<String> tableNames = tables.stream()
                .map(BookingTable::getTableName)
                .filter(name -> name != null)
                .collect(Collectors.toList());
        dto.setTableNames(tableNames);

        List<BookingDishDTO> dishes = bookingDishDetailRepository.findByBookingIdAndStoreId(e.getBookingId(), e.getStoreId())
                .stream()
                .map(d -> {
                    BookingDishDTO dishDTO = new BookingDishDTO();
                    dishDTO.setDishId(d.getDishId());
                    dishDTO.setDishName(d.getDishName());
                    dishDTO.setQuantity(d.getDishQuantity());
                    dishDTO.setUnitPrice(d.getUnitPrice());
                    dishDTO.setSubtotal(d.getSubtotal());
                    dishDTO.setNotes(d.getDishNote());
                    return dishDTO;
                })
                .collect(Collectors.toList());
        dto.setDishes(dishes);

        return dto;
    }
}

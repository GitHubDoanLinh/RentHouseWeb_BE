package com.example.renthouseweb_be.service;

import com.example.renthouseweb_be.exception.CommonException;
import com.example.renthouseweb_be.model.Booking;
import com.example.renthouseweb_be.model.BookingStatus;
import com.example.renthouseweb_be.model.House;
import com.example.renthouseweb_be.model.HouseStatus;
import com.example.renthouseweb_be.model.account.User;
import com.example.renthouseweb_be.repository.BookingRepository;
import com.example.renthouseweb_be.repository.HouseRepository;
import com.example.renthouseweb_be.repository.UserRepository;
import com.example.renthouseweb_be.requests.BookingRequest;
import com.example.renthouseweb_be.service.impl.HouseServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

@Service
public class BookingService {
    private final HouseServiceImpl houseService;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final HouseRepository houseRepository;
    public BookingService(HouseServiceImpl houseService, BookingRepository bookingRepository,
                          UserRepository userRepository,
                          HouseRepository houseRepository) {
        this.houseService = houseService;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.houseRepository = houseRepository;
    }
    public Booking save(BookingRequest request) throws CommonException {
        if (request.getStartDate() == null  || request.getEndDate() == null) {
            throw new CommonException("Ngày đặt phòng không được để trống");
        }
        Booking booking = new Booking();
        User user = userRepository.findById(request.getUserId()).orElseThrow(() -> new CommonException("Không tìm thấy tài khoản"));
        House house = houseRepository.findById(request.getHouseId()).orElseThrow(() -> new CommonException("Không tìm tấy nhà"));
        booking.setStartDate(request.getStartDate());
        booking.setEndDate(request.getEndDate());
        booking.setPrice(request.getPrice());
        booking.setCreateAt(LocalDateTime.now());
        booking.setNumberOfGuests(request.getNumberOfGuests());
        booking.setHouse(house);
        booking.setUser(user);
        booking.setStatus(BookingStatus.IN_PROGRESS);
        house.setStatus(HouseStatus.PREBOOK);
        houseService.save(house);
        return bookingRepository.save(booking);
    }

    public Iterable<Booking> findAllByUserId(Long userId,boolean deleteFlag){
        return bookingRepository.findAllByUserIdAndDeleteFlag(userId,deleteFlag);
    }
    public Iterable<Booking> findAllByHouseId(Long houseId, boolean deleteFlag) {
        return bookingRepository.findAllByHouseIdAndAndDeleteFlag(houseId, deleteFlag);
    }

    public void cancelBooking(Long idBooking) {
        Optional<Booking> booking = bookingRepository.findById(idBooking);
        if (booking.isPresent()) {
            Date currentDate = new Date();
            Date bookingDate = booking.get().getStartDate();
            LocalDate currentLocalDate = currentDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate bookingLocalDate = bookingDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (currentLocalDate.isBefore(bookingLocalDate.minusDays(1))) {
                booking.get().setDeleteFlag(true);
                bookingRepository.save(booking.get());
                House house = booking.get().getHouse();
                house.setStatus(HouseStatus.AVAILABLE);
                houseService.save(house);
            } else {
                throw new RuntimeException("Booking cannot be canceled. It's too close to the check-in date.");
            }
        }
    }
}
